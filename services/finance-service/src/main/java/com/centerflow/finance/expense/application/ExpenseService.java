package com.centerflow.finance.expense.application;

import com.centerflow.finance.common.api.PageResponse;
import com.centerflow.finance.common.exception.InvalidPaginationException;
import com.centerflow.finance.expense.api.dto.CancelExpenseRequest;
import com.centerflow.finance.expense.api.dto.CreateExpenseRequest;
import com.centerflow.finance.expense.api.dto.ExpenseResponse;
import com.centerflow.finance.expense.domain.Expense;
import com.centerflow.finance.expense.domain.ExpenseCategory;
import com.centerflow.finance.expense.domain.ExpenseStatus;
import com.centerflow.finance.expense.exception.ExpenseConflictException;
import com.centerflow.finance.expense.exception.ExpenseNotFoundException;
import com.centerflow.finance.expense.exception.InvalidExpenseException;
import com.centerflow.finance.expense.number.ExpenseNumberGenerator;
import com.centerflow.finance.expense.repository.ExpenseRepository;
import com.centerflow.finance.payment.domain.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
public class ExpenseService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ExpenseRepository expenseRepository;
    private final ExpenseNumberGenerator numberGenerator;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            ExpenseNumberGenerator numberGenerator
    ) {
        this.expenseRepository = expenseRepository;
        this.numberGenerator = numberGenerator;
    }

    @Transactional
    public ExpenseResponse createExpense(
            CreateExpenseRequest request
    ) {
        String externalReference =
                normalizeOptionalText(
                        request.externalReference()
                );

        if (
                externalReference != null
                        && expenseRepository
                        .existsByExternalReference(
                                externalReference
                        )
        ) {
            throw new ExpenseConflictException(
                    "Expense external reference "
                            + "already exists: "
                            + externalReference
            );
        }

        Expense expense = Expense.create(
                numberGenerator.nextNumber(),
                request.branchId(),
                request.category(),
                request.amount(),
                request.currency(),
                request.paymentMethod(),
                request.payee(),
                request.description(),
                request.expenseDate(),
                externalReference
        );

        Expense savedExpense =
                expenseRepository.saveAndFlush(expense);

        return ExpenseResponse.from(savedExpense);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(
            UUID expenseId
    ) {
        Expense expense = expenseRepository
                .findById(expenseId)
                .orElseThrow(
                        () -> new ExpenseNotFoundException(
                                expenseId
                        )
                );

        return ExpenseResponse.from(expense);
    }

    @Transactional
    public ExpenseResponse cancelExpense(
            UUID expenseId,
            CancelExpenseRequest request
    ) {
        Expense expense = expenseRepository
                .findByIdForUpdate(expenseId)
                .orElseThrow(
                        () -> new ExpenseNotFoundException(
                                expenseId
                        )
                );

        expense.cancel(request.reason());

        return ExpenseResponse.from(expense);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> searchExpenses(
            UUID branchId,
            ExpenseCategory category,
            ExpenseStatus status,
            PaymentMethod paymentMethod,
            LocalDate fromDate,
            LocalDate toDate,
            String payee,
            int page,
            int size
    ) {
        validatePagination(page, size);
        validateDateRange(fromDate, toDate);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("expenseDate"),
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Specification<Expense> specification =
                buildSpecification(
                        branchId,
                        category,
                        status,
                        paymentMethod,
                        fromDate,
                        toDate,
                        payee
                );

        Page<Expense> expensePage =
                expenseRepository.findAll(
                        specification,
                        pageRequest
                );

        return PageResponse.from(
                expensePage,
                ExpenseResponse::from
        );
    }

    private Specification<Expense> buildSpecification(
            UUID branchId,
            ExpenseCategory category,
            ExpenseStatus status,
            PaymentMethod paymentMethod,
            LocalDate fromDate,
            LocalDate toDate,
            String payee
    ) {
        Specification<Expense> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (branchId != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("branchId"),
                                    branchId
                            )
            );
        }

        if (category != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("category"),
                                    category
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

        if (paymentMethod != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("paymentMethod"),
                                    paymentMethod
                            )
            );
        }

        if (fromDate != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder
                                    .greaterThanOrEqualTo(
                                            root.<LocalDate>get(
                                                    "expenseDate"
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
                                                    "expenseDate"
                                            ),
                                            toDate
                                    )
            );
        }

        String normalizedPayee =
                normalizeOptionalText(payee);

        if (normalizedPayee != null) {
            String payeePattern =
                    "%"
                            + normalizedPayee
                            .toLowerCase(Locale.ROOT)
                            + "%";

            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(
                                            root.<String>get(
                                                    "payee"
                                            )
                                    ),
                                    payeePattern
                            )
            );
        }

        return specification;
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
            throw new InvalidExpenseException(
                    "Expense from date cannot be after to date"
            );
        }
    }

    private String normalizeOptionalText(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}