package com.centerflow.finance.account.api;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.Installment;
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
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InstallmentCollectionApiIntegrationTests {

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
    void processingShouldMarkOnlyPastInstallments()
            throws Exception {
        EnrollmentFinancialAccount account =
                createAccount("OVERDUE-PLAN-1");

        createInstallment(
                account,
                1,
                LocalDate.of(2026, 7, 1)
        );

        createInstallment(
                account,
                2,
                LocalDate.of(2026, 8, 1)
        );

        createInstallment(
                account,
                3,
                LocalDate.of(2026, 9, 1)
        );

        mockMvc.perform(
                        post(
                                "/api/v1/finance/internal/"
                                        + "installments/mark-overdue"
                        )
                                .param(
                                        "asOfDate",
                                        "2026-08-01"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.asOfDate")
                                .value("2026-08-01")
                )
                .andExpect(
                        jsonPath("$.markedOverdueCount")
                                .value(1)
                );

        mockMvc.perform(
                        get(
                                "/api/v1/finance/installments"
                        )
                                .param(
                                        "enrollmentId",
                                        account
                                                .getEnrollmentId()
                                                .toString()
                                )
                                .param(
                                        "status",
                                        "OVERDUE"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content", hasSize(1))
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].installmentNumber"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].remainingAmount"
                        ).value(300.00)
                );
    }

    @Test
    void searchShouldFilterByStudentAndDateRange()
            throws Exception {
        EnrollmentFinancialAccount targetAccount =
                createAccount("OVERDUE-PLAN-2");

        EnrollmentFinancialAccount otherAccount =
                createAccount("OVERDUE-PLAN-3");

        createInstallment(
                targetAccount,
                1,
                LocalDate.of(2026, 7, 10)
        );

        createInstallment(
                targetAccount,
                2,
                LocalDate.of(2026, 8, 10)
        );

        createInstallment(
                otherAccount,
                1,
                LocalDate.of(2026, 7, 15)
        );

        mockMvc.perform(
                        get(
                                "/api/v1/finance/installments"
                        )
                                .param(
                                        "studentId",
                                        targetAccount
                                                .getStudentId()
                                                .toString()
                                )
                                .param(
                                        "dueFrom",
                                        "2026-07-01"
                                )
                                .param(
                                        "dueTo",
                                        "2026-07-31"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content", hasSize(1))
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].studentId"
                        ).value(
                                targetAccount
                                        .getStudentId()
                                        .toString()
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].dueDate"
                        ).value("2026-07-10")
                );
    }

    @Test
    void searchShouldRejectInvalidDateRange()
            throws Exception {
        mockMvc.perform(
                        get(
                                "/api/v1/finance/installments"
                        )
                                .param(
                                        "dueFrom",
                                        "2026-09-01"
                                )
                                .param(
                                        "dueTo",
                                        "2026-08-01"
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Due date from must not be "
                                                + "after due date to"
                                )
                );
    }

    private EnrollmentFinancialAccount createAccount(
            String planCode
    ) {
        PricingPlan pricingPlan = PricingPlan.create(
                planCode,
                "Overdue Test Plan",
                "Plan used for installment collection tests",
                new BigDecimal("900.00"),
                "EGP",
                3,
                new BigDecimal("200.00")
        );

        pricingPlanRepository.saveAndFlush(
                pricingPlan
        );

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

        return financialAccountRepository.saveAndFlush(
                account
        );
    }

    private void createInstallment(
            EnrollmentFinancialAccount account,
            int installmentNumber,
            LocalDate dueDate
    ) {
        installmentRepository.saveAndFlush(
                Installment.create(
                        account.getId(),
                        installmentNumber,
                        dueDate,
                        new BigDecimal("300.00")
                )
        );
    }
}