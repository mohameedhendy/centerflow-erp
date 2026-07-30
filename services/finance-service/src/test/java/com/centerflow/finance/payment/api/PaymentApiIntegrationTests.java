package com.centerflow.finance.payment.api;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.Installment;
import com.centerflow.finance.account.domain.InstallmentScheduleGenerator;
import com.centerflow.finance.account.repository.EnrollmentFinancialAccountRepository;
import com.centerflow.finance.account.repository.InstallmentRepository;
import com.centerflow.finance.pricing.domain.PricingPlan;
import com.centerflow.finance.pricing.repository.PricingPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PricingPlanRepository pricingPlanRepository;

    @Autowired
    private EnrollmentFinancialAccountRepository
            financialAccountRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    @Test
    void paymentShouldAllocateAcrossOldestInstallments()
            throws Exception {
        EnrollmentFinancialAccount account =
                createAccount("PAYMENT-PLAN-1");

        String requestBody = """
                {
                  "amount": 500.00,
                  "method": "CASH",
                  "externalReference": "CASH-RECEIPT-1001"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/finance/"
                                        + "enrollment-accounts/"
                                        + "{enrollmentId}/payments",
                                account.getEnrollmentId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(
                        jsonPath("$.paymentNumber")
                                .value(
                                        org.hamcrest.Matchers
                                                .matchesPattern(
                                                        "PAY-\\d{4}-\\d{6}"
                                                )
                                )
                )
                .andExpect(
                        jsonPath("$.amount").value(500.00)
                )
                .andExpect(
                        jsonPath("$.method").value("CASH")
                )
                .andExpect(
                        jsonPath("$.accountPaidAmount")
                                .value(500.00)
                )
                .andExpect(
                        jsonPath("$.accountRemainingAmount")
                                .value(500.00)
                )
                .andExpect(
                        jsonPath("$.initialPaymentSatisfied")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.financialAccountStatus")
                                .value("OPEN")
                )
                .andExpect(
                        jsonPath("$.allocations", hasSize(2))
                )
                .andExpect(
                        jsonPath("$.allocations[0].amount")
                                .value(333.33)
                )
                .andExpect(
                        jsonPath("$.allocations[1].amount")
                                .value(166.67)
                );

        mockMvc.perform(
                        get(
                                "/api/v1/finance/"
                                        + "enrollment-accounts/"
                                        + "{enrollmentId}",
                                account.getEnrollmentId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.paidAmount")
                                .value(500.00)
                )
                .andExpect(
                        jsonPath("$.installments[0].status")
                                .value("PAID")
                )
                .andExpect(
                        jsonPath("$.installments[1].status")
                                .value("PARTIALLY_PAID")
                )
                .andExpect(
                        jsonPath(
                                "$.installments[1].paidAmount"
                        ).value(166.67)
                )
                .andExpect(
                        jsonPath("$.installments[2].status")
                                .value("PENDING")
                );
    }

    @Test
    void fullPaymentShouldSettleFinancialAccount()
            throws Exception {
        EnrollmentFinancialAccount account =
                createAccount("PAYMENT-PLAN-2");

        recordPayment(
                account.getEnrollmentId(),
                "500.00",
                "CARD",
                "CARD-1001"
        );

        mockMvc.perform(
                        post(
                                "/api/v1/finance/"
                                        + "enrollment-accounts/"
                                        + "{enrollmentId}/payments",
                                account.getEnrollmentId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        paymentBody(
                                                "500.00",
                                                "CARD",
                                                "CARD-1002"
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.accountPaidAmount")
                                .value(1000.00)
                )
                .andExpect(
                        jsonPath("$.accountRemainingAmount")
                                .value(0.00)
                )
                .andExpect(
                        jsonPath("$.financialAccountStatus")
                                .value("SETTLED")
                );
    }

    @Test
    void paymentShouldRejectAmountAboveRemainingBalance()
            throws Exception {
        EnrollmentFinancialAccount account =
                createAccount("PAYMENT-PLAN-3");

        mockMvc.perform(
                        post(
                                "/api/v1/finance/"
                                        + "enrollment-accounts/"
                                        + "{enrollmentId}/payments",
                                account.getEnrollmentId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        paymentBody(
                                                "1000.01",
                                                "CASH",
                                                "OVERPAYMENT-1"
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Payment amount exceeds "
                                                + "remaining balance"
                                )
                );
    }

    @Test
    void paymentShouldRejectDuplicateExternalReference()
            throws Exception {
        EnrollmentFinancialAccount account =
                createAccount("PAYMENT-PLAN-4");

        recordPayment(
                account.getEnrollmentId(),
                "100.00",
                "BANK_TRANSFER",
                "BANK-REFERENCE-1"
        );

        mockMvc.perform(
                        post(
                                "/api/v1/finance/"
                                        + "enrollment-accounts/"
                                        + "{enrollmentId}/payments",
                                account.getEnrollmentId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        paymentBody(
                                                "100.00",
                                                "BANK_TRANSFER",
                                                "BANK-REFERENCE-1"
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Payment external reference "
                                                + "already exists: "
                                                + "BANK-REFERENCE-1"
                                )
                );
    }

    @Test
    void paymentShouldReturn404ForUnknownEnrollment()
            throws Exception {
        UUID enrollmentId = UUID.randomUUID();

        mockMvc.perform(
                        post(
                                "/api/v1/finance/"
                                        + "enrollment-accounts/"
                                        + "{enrollmentId}/payments",
                                enrollmentId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        paymentBody(
                                                "100.00",
                                                "CASH",
                                                null
                                        )
                                )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void paymentShouldValidateRequiredFields()
            throws Exception {
        EnrollmentFinancialAccount account =
                createAccount("PAYMENT-PLAN-5");

        mockMvc.perform(
                        post(
                                "/api/v1/finance/"
                                        + "enrollment-accounts/"
                                        + "{enrollmentId}/payments",
                                account.getEnrollmentId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("Validation failed")
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.amount"
                        ).value("Payment amount is required")
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.method"
                        ).value("Payment method is required")
                );
    }

    private void recordPayment(
            UUID enrollmentId,
            String amount,
            String method,
            String externalReference
    ) throws Exception {
        mockMvc.perform(
                        post(
                                "/api/v1/finance/"
                                        + "enrollment-accounts/"
                                        + "{enrollmentId}/payments",
                                enrollmentId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        paymentBody(
                                                amount,
                                                method,
                                                externalReference
                                        )
                                )
                )
                .andExpect(status().isCreated());
    }

    private String paymentBody(
            String amount,
            String method,
            String externalReference
    ) {
        String referenceValue =
                externalReference == null
                        ? "null"
                        : "\""
                        + externalReference
                        + "\"";

        return """
                {
                  "amount": %s,
                  "method": "%s",
                  "externalReference": %s
                }
                """.formatted(
                amount,
                method,
                referenceValue
        );
    }

    private EnrollmentFinancialAccount createAccount(
            String pricingPlanCode
    ) {
        PricingPlan pricingPlan = PricingPlan.create(
                pricingPlanCode,
                "Payment Test Plan",
                "Plan for payment tests",
                new BigDecimal("1000.00"),
                "EGP",
                3,
                new BigDecimal("300.00")
        );

        pricingPlanRepository.saveAndFlush(pricingPlan);

        EnrollmentFinancialAccount account =
                EnrollmentFinancialAccount.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        pricingPlan.getId(),
                        pricingPlan.getCode(),
                        pricingPlan.getTotalAmount(),
                        pricingPlan.getCurrency(),
                        pricingPlan.getInstallmentCount(),
                        pricingPlan
                                .getInitialPaymentAmount()
                );

        financialAccountRepository.saveAndFlush(account);

        List<Installment> installments =
                InstallmentScheduleGenerator
                        .generate(
                                account.getTotalAmount(),
                                account.getInstallmentCount(),
                                LocalDate.of(2026, 8, 1)
                        )
                        .stream()
                        .map(item ->
                                Installment.create(
                                        account.getId(),
                                        item.installmentNumber(),
                                        item.dueDate(),
                                        item.amount()
                                )
                        )
                        .toList();

        installmentRepository.saveAllAndFlush(
                installments
        );

        return account;
    }
}