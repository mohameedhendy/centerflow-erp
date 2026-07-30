package com.centerflow.finance.account.application;

import com.centerflow.finance.account.api.dto.InstallmentCollectionResponse;
import com.centerflow.finance.account.api.dto.OverdueProcessingResponse;
import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.Installment;
import com.centerflow.finance.account.domain.InstallmentStatus;
import com.centerflow.finance.account.exception.InvalidInstallmentSearchException;
import com.centerflow.finance.account.repository.EnrollmentFinancialAccountRepository;
import com.centerflow.finance.account.repository.InstallmentRepository;
import com.centerflow.finance.common.api.PageResponse;
import com.centerflow.finance.common.exception.InvalidPaginationException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InstallmentCollectionService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final EnumSet<InstallmentStatus>
            OVERDUE_ELIGIBLE_STATUSES = EnumSet.of(
                    InstallmentStatus.PENDING,
                    InstallmentStatus.PARTIALLY_PAID
            );

    private final InstallmentRepository installmentRepository;

    private final EnrollmentFinancialAccountRepository
            financialAccountRepository;

    public InstallmentCollectionService(
            InstallmentRepository installmentRepository,
            EnrollmentFinancialAccountRepository
                    financialAccountRepository
    ) {
        this.installmentRepository = installmentRepository;
        this.financialAccountRepository =
                financialAccountRepository;
    }

    @Transactional
    public OverdueProcessingResponse markOverdueInstallments(
            LocalDate asOfDate
    ) {
        List<Installment> installments =
                installmentRepository
                        .findAllEligibleForOverdueUpdate(
                                asOfDate,
                                OVERDUE_ELIGIBLE_STATUSES
                        );

        int markedCount = 0;

        for (Installment installment : installments) {
            if (installment.markOverdue(asOfDate)) {
                markedCount++;
            }
        }

        return new OverdueProcessingResponse(
                asOfDate,
                markedCount
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<InstallmentCollectionResponse>
    searchInstallments(
            UUID enrollmentId,
            UUID studentId,
            InstallmentStatus status,
            LocalDate dueFrom,
            LocalDate dueTo,
            int page,
            int size
    ) {
        validatePagination(page, size);
        validateDateRange(dueFrom, dueTo);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("dueDate"),
                        Sort.Order.asc("installmentNumber"),
                        Sort.Order.asc("id")
                )
        );

        List<UUID> accountIds = resolveAccountIds(
                enrollmentId,
                studentId
        );

        if (accountIds != null && accountIds.isEmpty()) {
            return PageResponse.from(
                    Page.empty(pageRequest),
                    installment -> {
                        throw new IllegalStateException(
                                "Empty page cannot be mapped"
                        );
                    }
            );
        }

        Specification<Installment> specification =
                buildSpecification(
                        accountIds,
                        status,
                        dueFrom,
                        dueTo
                );

        Page<Installment> installmentPage =
                installmentRepository.findAll(
                        specification,
                        pageRequest
                );

        List<UUID> pageAccountIds =
                installmentPage.getContent()
                        .stream()
                        .map(Installment::getFinancialAccountId)
                        .distinct()
                        .toList();

        Map<UUID, EnrollmentFinancialAccount>
                accountsById =
                financialAccountRepository
                        .findAllById(pageAccountIds)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        EnrollmentFinancialAccount
                                                ::getId,
                                        Function.identity()
                                )
                        );

        return PageResponse.from(
                installmentPage,
                installment -> {
                    EnrollmentFinancialAccount account =
                            accountsById.get(
                                    installment
                                            .getFinancialAccountId()
                            );

                    if (account == null) {
                        throw new IllegalStateException(
                                "Installment financial account "
                                        + "does not exist"
                        );
                    }

                    return InstallmentCollectionResponse.from(
                            installment,
                            account
                    );
                }
        );
    }

    private List<UUID> resolveAccountIds(
            UUID enrollmentId,
            UUID studentId
    ) {
        if (enrollmentId != null) {
            return financialAccountRepository
                    .findByEnrollmentId(enrollmentId)
                    .filter(account ->
                            studentId == null
                                    || account
                                    .getStudentId()
                                    .equals(studentId)
                    )
                    .map(account ->
                            List.of(account.getId())
                    )
                    .orElseGet(List::of);
        }

        if (studentId != null) {
            return financialAccountRepository
                    .findIdsByStudentId(studentId);
        }

        return null;
    }

    private Specification<Installment>
    buildSpecification(
            List<UUID> accountIds,
            InstallmentStatus status,
            LocalDate dueFrom,
            LocalDate dueTo
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates =
                    new ArrayList<>();

            if (accountIds != null) {
                predicates.add(
                        root.get("financialAccountId")
                                .in(accountIds)
                );
            }

            if (status != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("status"),
                                status
                        )
                );
            }

            if (dueFrom != null) {
                predicates.add(
                        criteriaBuilder
                                .greaterThanOrEqualTo(
                                        root.get("dueDate"),
                                        dueFrom
                                )
                );
            }

            if (dueTo != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("dueDate"),
                                dueTo
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            Predicate[]::new
                    )
            );
        };
    }

    private void validateDateRange(
            LocalDate dueFrom,
            LocalDate dueTo
    ) {
        if (
                dueFrom != null
                        && dueTo != null
                        && dueFrom.isAfter(dueTo)
        ) {
            throw new InvalidInstallmentSearchException(
                    "Due date from must not be after due date to"
            );
        }
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new InvalidPaginationException(
                    "Page index must be zero or greater"
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPaginationException(
                    "Page size must be between 1 and "
                            + MAX_PAGE_SIZE
            );
        }
    }
}