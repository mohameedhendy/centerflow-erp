package com.centerflow.finance.adjustment;

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
class FinancialAdjustmentApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnrollmentFinancialAccountRepository
            accountRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    @Autowired
    private PricingPlanRepository pricingPlanRepository;

    @Test
    void adjustmentApisShouldRecordAndSearchDiscount()
            throws Exception {
        EnrollmentFinancialAccount account =
                createFinancialAccount();

        String requestBody = """
                {
                  "type": "DISCOUNT",
                  "amount": 125.00,
                  "reason": "Early registration discount",
                  "externalReference": "%s"
                }
                """.formatted(
                "API-DISCOUNT-" + UUID.randomUUID()
        );

        mockMvc.perform(
                        post(
                                "/api/v1/finance/enrollment-accounts/{enrollmentId}/adjustments",
                                account.getEnrollmentId()
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().exists("Location")
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("DISCOUNT")
                )
                .andExpect(
                        jsonPath("$.amount")
                                .value(125.00)
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("EGP")
                );

        mockMvc.perform(
                        get(
                                "/api/v1/finance/enrollment-accounts/{enrollmentId}/adjustments",
                                account.getEnrollmentId()
                        )
                                .param(
                                        "type",
                                        "DISCOUNT"
                                )
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.content",
                                hasSize(1)
                        )
                )
                .andExpect(
                        jsonPath("$.content[0].type")
                                .value("DISCOUNT")
                );
    }

    @Test
    void invalidAdjustmentShouldReturnBadRequest()
            throws Exception {
        EnrollmentFinancialAccount account =
                createFinancialAccount();

        String requestBody = """
                {
                  "type": "DISCOUNT",
                  "amount": 0,
                  "reason": "No",
                  "externalReference": "INVALID-ADJUSTMENT"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/finance/enrollment-accounts/{enrollmentId}/adjustments",
                                account.getEnrollmentId()
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }

    private EnrollmentFinancialAccount
    createFinancialAccount() {
        PricingPlan pricingPlan = PricingPlan.create(
                "API-ADJ-"
                        + UUID.randomUUID()
                        .toString()
                        .substring(0, 12),
                "API Adjustment Plan",
                "Plan used for API adjustment tests",
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
                        pricingPlan.getInitialPaymentAmount()
                );

        accountRepository.saveAndFlush(account);

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