package com.centerflow.finance.adjustment.application;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.Installment;
import com.centerflow.finance.account.domain.InstallmentStatus;
import com.centerflow.finance.account.exception.EnrollmentFinancialAccountNotFoundException;
import com.centerflow.finance.account.repository.EnrollmentFinancialAccountRepository;
import com.centerflow.finance.account.repository.InstallmentRepository;
import com.centerflow.finance.adjustment.api.dto.FinancialAdjustmentResponse;
import com.centerflow.finance.adjustment.api.dto.RecordFinancialAdjustmentRequest;
import com.centerflow.finance.adjustment.domain.FinancialAdjustment;
import com.centerflow.finance.adjustment.domain.FinancialAdjustmentType;
import com.centerflow.finance.adjustment.exception.FinancialAdjustmentConflictException;
import com.centerflow.finance.adjustment.exception.FinancialAdjustmentNotFoundException;
import com.centerflow.finance.adjustment.repository.FinancialAdjustmentRepository;
import com.centerflow.finance.common.api.PageResponse;
import com.centerflow.finance.common.exception.InvalidPaginationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class FinancialAdjustmentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final EnrollmentFinancialAccountRepository
            financialAccountRepository;

    private final InstallmentRepository installmentRepository;

    private final FinancialAdjustmentRepository
            adjustmentRepository;

    public FinancialAdjustmentService(
            EnrollmentFinancialAccountRepository
                    financialAccountRepository,
            InstallmentRepository installmentRepository,
            FinancialAdjustmentRepository
                    adjustmentRepository
    ) {
        this.financialAccountRepository =
                financialAccountRepository;

        this.installmentRepository =
                installmentRepository;

        this.adjustmentRepository =
                adjustmentRepository;
    }

    @Transactional
    public FinancialAdjustmentResponse recordAdjustment(
            UUID enrollmentId,
            RecordFinancialAdjustmentRequest request
    ) {
        EnrollmentFinancialAccount account =
                financialAccountRepository
                        .findByEnrollmentIdForUpdate(
                                enrollmentId
                        )
                        .orElseThrow(
                                () ->
                                        new EnrollmentFinancialAccountNotFoundException(
                                                enrollmentId
                                        )
                        );

        String externalReference =
                normalizeExternalReference(
                        request.externalReference()
                );

        if (
                externalReference != null
                        && adjustmentRepository
                        .existsByExternalReference(
                                externalReference
                        )
        ) {
            throw new FinancialAdjustmentConflictException(
                    "Adjustment external reference "
                            + "already exists: "
                            + externalReference
            );
        }

        FinancialAdjustment adjustment =
                FinancialAdjustment.create(
                        account.getId(),
                        request.type(),
                        request.amount(),
                        account.getCurrency(),
                        request.reason(),
                        externalReference
                );

        List<Installment> installments =
                installmentRepository
                        .findAllByFinancialAccountIdForUpdate(
                                account.getId()
                        );

        if (
                adjustment.getType()
                        == FinancialAdjustmentType.DISCOUNT
        ) {
            applyDiscount(
                    account,
                    installments,
                    adjustment.getAmount()
            );
        }

        if (
                adjustment.getType()
                        == FinancialAdjustmentType.CHARGE
        ) {
            applyCharge(
                    account,
                    installments,
                    adjustment.getAmount()
            );
        }

        FinancialAdjustment savedAdjustment =
                adjustmentRepository.saveAndFlush(
                        adjustment
                );

        return FinancialAdjustmentResponse.from(
                savedAdjustment
        );
    }

    @Transactional(readOnly = true)
    public FinancialAdjustmentResponse getAdjustment(
            UUID adjustmentId
    ) {
        FinancialAdjustment adjustment =
                adjustmentRepository
                        .findById(adjustmentId)
                        .orElseThrow(
                                () ->
                                        new FinancialAdjustmentNotFoundException(
                                                adjustmentId
                                        )
                        );

        return FinancialAdjustmentResponse.from(adjustment);
    }

    @Transactional(readOnly = true)
    public PageResponse<FinancialAdjustmentResponse>
    searchAdjustments(
            UUID enrollmentId,
            FinancialAdjustmentType type,
            int page,
            int size
    ) {
        validatePagination(page, size);

        EnrollmentFinancialAccount account =
                financialAccountRepository
                        .findByEnrollmentId(enrollmentId)
                        .orElseThrow(
                                () ->
                                        new EnrollmentFinancialAccountNotFoundException(
                                                enrollmentId
                                        )
                        );

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<FinancialAdjustment> adjustmentPage;

        if (type == null) {
            adjustmentPage =
                    adjustmentRepository
                            .findAllByFinancialAccountId(
                                    account.getId(),
                                    pageRequest
                            );
        }
        else {
            adjustmentPage =
                    adjustmentRepository
                            .findAllByFinancialAccountIdAndType(
                                    account.getId(),
                                    type,
                                    pageRequest
                            );
        }

        return PageResponse.from(
                adjustmentPage,
                FinancialAdjustmentResponse::from
        );
    }

    private void applyDiscount(
            EnrollmentFinancialAccount account,
            List<Installment> installments,
            BigDecimal discountAmount
    ) {
        account.applyDiscount(discountAmount);

        BigDecimal remainingDiscount =
                discountAmount;

        List<Installment> reversedInstallments =
                installments.stream()
                        .sorted(
                                Comparator.comparingInt(
                                        Installment
                                                ::getInstallmentNumber
                                ).reversed()
                        )
                        .toList();

        LocalDate accountingDate =
                LocalDate.now(ZoneOffset.UTC);

        for (
                Installment installment
                : reversedInstallments
        ) {
            if (remainingDiscount.signum() == 0) {
                break;
            }

            if (
                    installment.getStatus()
                            == InstallmentStatus.CANCELLED
            ) {
                continue;
            }

            BigDecimal availableAmount =
                    installment.getRemainingAmount();

            if (availableAmount.signum() == 0) {
                continue;
            }

            BigDecimal appliedAmount =
                    remainingDiscount.min(
                            availableAmount
                    );

            installment.applyDiscount(
                    appliedAmount,
                    accountingDate
            );

            remainingDiscount =
                    remainingDiscount.subtract(
                            appliedAmount
                    );
        }

        if (remainingDiscount.signum() != 0) {
            throw new FinancialAdjustmentConflictException(
                    "Discount amount exceeds the "
                            + "adjustable installment balance"
            );
        }
    }

    private void applyCharge(
            EnrollmentFinancialAccount account,
            List<Installment> installments,
            BigDecimal chargeAmount
    ) {
        Installment lastInstallment =
                installments.stream()
                        .filter(
                                installment ->
                                        installment.getStatus()
                                                != InstallmentStatus
                                                .CANCELLED
                        )
                        .max(
                                Comparator.comparingInt(
                                        Installment
                                                ::getInstallmentNumber
                                )
                        )
                        .orElseThrow(
                                () ->
                                        new FinancialAdjustmentConflictException(
                                                "No active installment "
                                                        + "is available"
                                        )
                        );

        account.applyCharge(chargeAmount);

        lastInstallment.applyCharge(
                chargeAmount,
                LocalDate.now(ZoneOffset.UTC)
        );
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

    private String normalizeExternalReference(
            String externalReference
    ) {
        if (
                externalReference == null
                        || externalReference.isBlank()
        ) {
            return null;
        }

        return externalReference.trim();
    }
}