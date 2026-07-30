package com.centerflow.finance.expense.api;

import com.centerflow.finance.common.api.PageResponse;
import com.centerflow.finance.expense.api.dto.CancelExpenseRequest;
import com.centerflow.finance.expense.api.dto.CreateExpenseRequest;
import com.centerflow.finance.expense.api.dto.ExpenseResponse;
import com.centerflow.finance.expense.application.ExpenseService;
import com.centerflow.finance.expense.domain.ExpenseCategory;
import com.centerflow.finance.expense.domain.ExpenseStatus;
import com.centerflow.finance.payment.domain.PaymentMethod;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(
            ExpenseService expenseService
    ) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid
            @RequestBody
            CreateExpenseRequest request
    ) {
        ExpenseResponse response =
                expenseService.createExpense(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{expenseId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> getExpense(
            @PathVariable UUID expenseId
    ) {
        return ResponseEntity.ok(
                expenseService.getExpense(expenseId)
        );
    }

    @PostMapping("/{expenseId}/cancel")
    public ResponseEntity<ExpenseResponse> cancelExpense(
            @PathVariable UUID expenseId,

            @Valid
            @RequestBody
            CancelExpenseRequest request
    ) {
        return ResponseEntity.ok(
                expenseService.cancelExpense(
                        expenseId,
                        request
                )
        );
    }

    @GetMapping
    public ResponseEntity<PageResponse<ExpenseResponse>>
    searchExpenses(
            @RequestParam(required = false)
            UUID branchId,

            @RequestParam(required = false)
            ExpenseCategory category,

            @RequestParam(required = false)
            ExpenseStatus status,

            @RequestParam(required = false)
            PaymentMethod paymentMethod,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate toDate,

            @RequestParam(required = false)
            String payee,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        return ResponseEntity.ok(
                expenseService.searchExpenses(
                        branchId,
                        category,
                        status,
                        paymentMethod,
                        fromDate,
                        toDate,
                        payee,
                        page,
                        size
                )
        );
    }
}