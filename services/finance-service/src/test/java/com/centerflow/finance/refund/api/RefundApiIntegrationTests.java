package com.centerflow.finance.refund.api;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.Installment;
import com.centerflow.finance.account.domain.InstallmentScheduleGenerator;
import com.centerflow.finance.account.repository.EnrollmentFinancialAccountRepository;
import com.centerflow.finance.account.repository.InstallmentRepository;
import com.centerflow.finance.payment.domain.Payment;
import com.centerflow.finance.payment.repository.PaymentRepository;
import com.centerflow.finance.pricing.domain.PricingPlan;
import com.centerflow.finance.pricing.repository.PricingPlanRepository;
import com.centerflow.finance.refund.domain.Refund;
import com.centerflow.finance.refund.repository.RefundRepository;
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
class RefundApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PricingPlanRepository pricingPlanRepository;

    @Autowired
    private EnrollmentFinancialAccountRepository
            financialAccountRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Test
    void partialRefundShouldReversePaymentAllocations()
            throws Exception {
        EnrollmentFinancialAccount account =
                createAccount("REFUND-PLAN-1");

        recordPayment(
                account.getEnrollmentId(),
                "500.00",
                "PAYMENT-REF-1"
        );

        Payment payment = paymentRepository
                .findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        mockMvc.perform(
                        post(
                                "/api/v1/finance/payments/"
                                        + "{paymentId}/refunds",
                                payment.getId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        refundBody(
                                                "200.00",
                                                "Partial cancellation",
                                                "REFUND-REF-1"
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(
                        jsonPath("$.refundNumber")
                                .value(
                                        org.hamcrest.Matchers
                                                .matchesPattern(
                                                        "REF-\\d{4}-\\d{6}"
                                                )
                                )
                )
                .andExpect(
                        jsonPath("$.amount").value(200.00)
                )
                .andExpect(
                        jsonPath("$.paymentRefundedAmount")
                                .value(200.00)
                )
                .andExpect(
                        jsonPath("$.paymentRefundableAmount")
                                .value(300.00)
                )
                .andExpect(
                        jsonPath("$.paymentStatus")
                                .value("PARTIALLY_REFUNDED")
                )
                .andExpect(
                        jsonPath("$.accountPaidAmount")
                                .value(300.00)
                )
                .andExpect(
                        jsonPath("$.accountRemainingAmount")
                                .value(700.00)
                )
                .andExpect(
                        jsonPath("$.initialPaymentSatisfied")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.allocations", hasSize(2))
                )
                .andExpect(
                        jsonPath("$.allocations[0].amount")
                                .value(166.67)
                )
                .andExpect(
                        jsonPath("$.allocations[1].amount")
                                .value(33.33)
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
                                .value(300.00)
                )
                .andExpect(
                        jsonPath(
                                "$.installments[0].paidAmount"
                        ).value(300.00)
                )
                .andExpect(
                        jsonPath(
                                "$.installments[0].status"
                        ).value("PARTIALLY_PAID")
                )
                .andExpect(
                        jsonPath(
                                "$.installments[1].paidAmount"
                        ).value(0.00)
                )
                .andExpect(
                        jsonPath(
                                "$.installments[1].status"
                        ).value("PENDING")
                );
    }

    @Test
    void fullRefundShouldMarkPaymentRefunded()
            throws Exception {
        EnrollmentFinancialAccount account =
                createAccount("REFUND-PLAN-2");

        recordPayment(
                account.getEnrollmentId(),
                "500.00",
                "PAYMENT-REF-2"
        );

        Payment payment = paymentRepository
                .findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        recordRefund(
                payment.getId(),
                "200.00",
                "First refund",
                "REFUND-REF-2A"
        );

        mockMvc.perform(
                        post(
                                "/api/v1/finance/payments/"
                                        + "{paymentId}/refunds",
                                payment.getId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        refundBody(
                                                "300.00",
                                                "Final refund",
                                                "REFUND-REF-2B"
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.paymentRefundedAmount")
                                .value(500.00)
                )
                .andExpect(
                        jsonPath("$.paymentRefundableAmount")
                                .value(0.00)
                )
                .andExpect(
                        jsonPath("$.paymentStatus")
                                .value("REFUNDED")
                )
                .andExpect(
                        jsonPath("$.accountPaidAmount")
                                .value(0.00)
                )
                .andExpect(
                        jsonPath("$.initialPaymentSatisfied")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.financialAccountStatus")
                                .value("OPEN")
                );
    }

    @Test
    void refundShouldRejectAmountAboveRefundableBalance()
            throws Exception {
        EnrollmentFinancialAccount account =
                createAccount("REFUND-PLAN-3");

        recordPayment(
                account.getEnrollmentId(),
                "500.00",
                "PAYMENT-REF-3"
        );

        Payment payment = paymentRepository
                .findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        mockMvc.perform(
                        post(
                                "/api/v1/finance/payments/"
                                        + "{paymentId}/refunds",
                                payment.getId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        refundBody(
                                                "500.01",
                                                "Invalid refund",
                                                "REFUND-REF-3"
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Refund amount exceeds payment "
                                                + "refundable amount"
                                )
                );
    }

    @Test
    void refundShouldRejectDuplicateExternalReference()
            throws Exception {
        EnrollmentFinancialAccount account =
                createAccount("REFUND-PLAN-4");

        recordPayment(
                account.getEnrollmentId(),
                "500.00",
                "PAYMENT-REF-4"
        );

        Payment payment = paymentRepository
                .findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        recordRefund(
                payment.getId(),
                "100.00",
                "First refund",
                "DUPLICATE-REFUND"
        );

        mockMvc.perform(
                        post(
                                "/api/v1/finance/payments/"
                                        + "{paymentId}/refunds",
                                payment.getId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        refundBody(
                                                "100.00",
                                                "Duplicate reference",
                                                "DUPLICATE-REFUND"
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Refund external reference "
                                                + "already exists: "
                                                + "DUPLICATE-REFUND"
                                )
                );
    }

    @Test
    void getShouldReturnSavedRefund()
            throws Exception {
        EnrollmentFinancialAccount account =
                createAccount("REFUND-PLAN-5");

        recordPayment(
                account.getEnrollmentId(),
                "300.00",
                "PAYMENT-REF-5"
        );

        Payment payment = paymentRepository
                .findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        recordRefund(
                payment.getId(),
                "100.00",
                "Saved refund",
                "REFUND-GET-1"
        );

        Refund refund = refundRepository
                .findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        mockMvc.perform(
                        get(
                                "/api/v1/finance/refunds/{refundId}",
                                refund.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(refund.getId().toString())
                )
                .andExpect(
                        jsonPath("$.amount").value(100.00)
                )
                .andExpect(
                        jsonPath("$.reason")
                                .value("Saved refund")
                );
    }

    @Test
    void refundShouldReturn404ForUnknownPayment()
            throws Exception {
        UUID paymentId = UUID.randomUUID();

        mockMvc.perform(
                        post(
                                "/api/v1/finance/payments/"
                                        + "{paymentId}/refunds",
                                paymentId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        refundBody(
                                                "100.00",
                                                "Unknown payment",
                                                null
                                        )
                                )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void refundShouldValidateRequiredFields()
            throws Exception {
        EnrollmentFinancialAccount account =
                createAccount("REFUND-PLAN-6");

        recordPayment(
                account.getEnrollmentId(),
                "300.00",
                "PAYMENT-REF-6"
        );

        Payment payment = paymentRepository
                .findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        mockMvc.perform(
                        post(
                                "/api/v1/finance/payments/"
                                        + "{paymentId}/refunds",
                                payment.getId()
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
                        ).value("Refund amount is required")
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.reason"
                        ).value("Refund reason is required")
                );
    }

    private void recordPayment(
            UUID enrollmentId,
            String amount,
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
                                        """
                                        {
                                          "amount": %s,
                                          "method": "CASH",
                                          "externalReference": "%s"
                                        }
                                        """.formatted(
                                                amount,
                                                externalReference
                                        )
                                )
                )
                .andExpect(status().isCreated());
    }

    private void recordRefund(
            UUID paymentId,
            String amount,
            String reason,
            String externalReference
    ) throws Exception {
        mockMvc.perform(
                        post(
                                "/api/v1/finance/payments/"
                                        + "{paymentId}/refunds",
                                paymentId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        refundBody(
                                                amount,
                                                reason,
                                                externalReference
                                        )
                                )
                )
                .andExpect(status().isCreated());
    }

    private String refundBody(
            String amount,
            String reason,
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
                  "reason": "%s",
                  "externalReference": %s
                }
                """.formatted(
                amount,
                reason,
                referenceValue
        );
    }

    private EnrollmentFinancialAccount createAccount(
            String pricingPlanCode
    ) {
        PricingPlan pricingPlan = PricingPlan.create(
                pricingPlanCode,
                "Refund Test Plan",
                "Plan for refund tests",
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