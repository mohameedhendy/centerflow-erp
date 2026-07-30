package com.centerflow.finance.earning.application;

import com.centerflow.finance.common.api.PageResponse;
import com.centerflow.finance.common.exception.InvalidPaginationException;
import com.centerflow.finance.earning.api.dto.CancelInstructorEarningRequest;
import com.centerflow.finance.earning.api.dto.CreateInstructorEarningRequest;
import com.centerflow.finance.earning.api.dto.InstructorEarningResponse;
import com.centerflow.finance.earning.api.dto.PayInstructorEarningRequest;
import com.centerflow.finance.earning.domain.InstructorEarning;
import com.centerflow.finance.earning.domain.InstructorEarningStatus;
import com.centerflow.finance.earning.exception.InstructorEarningConflictException;
import com.centerflow.finance.earning.exception.InstructorEarningNotFoundException;
import com.centerflow.finance.earning.exception.InvalidInstructorEarningException;
import com.centerflow.finance.earning.number.InstructorEarningNumberGenerator;
import com.centerflow.finance.earning.repository.InstructorEarningRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
public class InstructorEarningService {

    private static final int MAX_PAGE_SIZE = 100;

    private final InstructorEarningRepository
            earningRepository;

    private final InstructorEarningNumberGenerator
            numberGenerator;

    public InstructorEarningService(
            InstructorEarningRepository earningRepository,
            InstructorEarningNumberGenerator numberGenerator
    ) {
        this.earningRepository = earningRepository;
        this.numberGenerator = numberGenerator;
    }

    @Transactional
    public InstructorEarningResponse recordEarning(
            CreateInstructorEarningRequest request
    ) {
        InstructorEarning existingEarning =
                earningRepository
                        .findBySessionId(request.sessionId())
                        .orElse(null);

        if (existingEarning != null) {
            validateIdempotentRequest(
                    existingEarning,
                    request
            );

            return InstructorEarningResponse.from(
                    existingEarning
            );
        }

        InstructorEarning earning =
                InstructorEarning.create(
                        numberGenerator.nextNumber(),
                        request.instructorId(),
                        request.sessionId(),
                        request.batchId(),
                        request.amount(),
                        request.currency(),
                        request.sessionDate(),
                        request.description()
                );

        InstructorEarning savedEarning =
                earningRepository.saveAndFlush(earning);

        return InstructorEarningResponse.from(
                savedEarning
        );
    }

    @Transactional(readOnly = true)
    public InstructorEarningResponse getEarning(
            UUID earningId
    ) {
        InstructorEarning earning =
                earningRepository
                        .findById(earningId)
                        .orElseThrow(
                                () ->
                                        new InstructorEarningNotFoundException(
                                                earningId
                                        )
                        );

        return InstructorEarningResponse.from(earning);
    }

    @Transactional
    public InstructorEarningResponse payEarning(
            UUID earningId,
            PayInstructorEarningRequest request
    ) {
        InstructorEarning earning =
                earningRepository
                        .findByIdForUpdate(earningId)
                        .orElseThrow(
                                () ->
                                        new InstructorEarningNotFoundException(
                                                earningId
                                        )
                        );

        String paymentReference =
                request.paymentReference().trim();

        if (
                earning.getStatus()
                        != InstructorEarningStatus.PAID
                        && earningRepository
                        .existsByPaymentReference(
                                paymentReference
                        )
        ) {
            throw new InstructorEarningConflictException(
                    "Instructor earning payment reference "
                            + "already exists: "
                            + paymentReference
            );
        }

        if (
                earning.getStatus()
                        == InstructorEarningStatus.PAID
                        && !paymentReference.equals(
                                earning.getPaymentReference()
                        )
        ) {
            throw new InstructorEarningConflictException(
                    "Instructor earning was already paid "
                            + "using another reference"
            );
        }

        earning.markPaid(
                request.paymentMethod(),
                paymentReference
        );

        return InstructorEarningResponse.from(earning);
    }

    @Transactional
    public InstructorEarningResponse cancelEarning(
            UUID earningId,
            CancelInstructorEarningRequest request
    ) {
        InstructorEarning earning =
                earningRepository
                        .findByIdForUpdate(earningId)
                        .orElseThrow(
                                () ->
                                        new InstructorEarningNotFoundException(
                                                earningId
                                        )
                        );

        earning.cancel(request.reason());

        return InstructorEarningResponse.from(earning);
    }

    @Transactional(readOnly = true)
    public PageResponse<InstructorEarningResponse>
    searchEarnings(
            UUID instructorId,
            UUID batchId,
            InstructorEarningStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        validatePagination(page, size);
        validateDateRange(fromDate, toDate);

        Specification<InstructorEarning> specification =
                buildSpecification(
                        instructorId,
                        batchId,
                        status,
                        fromDate,
                        toDate
                );

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("sessionDate"),
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<InstructorEarning> earningPage =
                earningRepository.findAll(
                        specification,
                        pageRequest
                );

        return PageResponse.from(
                earningPage,
                InstructorEarningResponse::from
        );
    }

    private Specification<InstructorEarning>
    buildSpecification(
            UUID instructorId,
            UUID batchId,
            InstructorEarningStatus status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Specification<InstructorEarning> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (instructorId != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("instructorId"),
                                    instructorId
                            )
            );
        }

        if (batchId != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("batchId"),
                                    batchId
                            )
            );
        }

        if (status != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("status"),
                                    status
                            )
            );
        }

        if (fromDate != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder
                                    .greaterThanOrEqualTo(
                                            root.<LocalDate>get(
                                                    "sessionDate"
                                            ),
                                            fromDate
                                    )
            );
        }

        if (toDate != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder
                                    .lessThanOrEqualTo(
                                            root.<LocalDate>get(
                                                    "sessionDate"
                                            ),
                                            toDate
                                    )
            );
        }

        return specification;
    }

    private void validateIdempotentRequest(
            InstructorEarning existing,
            CreateInstructorEarningRequest request
    ) {
        BigDecimal requestedAmount =
                request.amount().setScale(2);

        String requestedCurrency =
                request.currency()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        String requestedDescription =
                request.description().trim();

        boolean sameRequest =
                existing.getInstructorId()
                        .equals(request.instructorId())
                        && existing.getBatchId()
                        .equals(request.batchId())
                        && existing.getAmount()
                        .compareTo(requestedAmount) == 0
                        && existing.getCurrency()
                        .equals(requestedCurrency)
                        && existing.getSessionDate()
                        .equals(request.sessionDate())
                        && existing.getDescription()
                        .equals(requestedDescription);

        if (!sameRequest) {
            throw new InstructorEarningConflictException(
                    "Session already has a different "
                            + "instructor earning record: "
                            + request.sessionId()
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

    private void validateDateRange(
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (
                fromDate != null
                        && toDate != null
                        && fromDate.isAfter(toDate)
        ) {
            throw new InvalidInstructorEarningException(
                    "Session from date cannot be "
                            + "after to date"
            );
        }
    }
}