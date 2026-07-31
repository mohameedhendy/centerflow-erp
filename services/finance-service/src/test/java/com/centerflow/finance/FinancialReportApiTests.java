package com.centerflow.finance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Sql("/sql/financial-report-fixtures.sql")
class FinancialReportApiTests {

    private static final String REPORTS_URL =
            "/api/v1/finance/reports";

    private static final UUID FINANCIAL_ACCOUNT_ID =
            UUID.fromString(
                    "00000000-0000-0000-0000-000000000201"
            );

    @Autowired
    private MockMvc mockMvc;

    @Test
    void overviewShouldAggregateFinancialData()
            throws Exception {
        mockMvc.perform(
                        get(REPORTS_URL + "/overview")
                                .param("currency", "egp")
                                .param(
                                        "fromDate",
                                        "2026-07-31"
                                )
                                .param(
                                        "toDate",
                                        "2026-07-31"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.currency")
                                .value("EGP")
                )
                .andExpect(
                        jsonPath(
                                "$.accounts.totalAccounts"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.accounts.openAccounts"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.accounts.billedAmount"
                        ).value(1100.00)
                )
                .andExpect(
                        jsonPath(
                                "$.accounts.paidAmount"
                        ).value(600.00)
                )
                .andExpect(
                        jsonPath(
                                "$.accounts.outstandingAmount"
                        ).value(500.00)
                )
                .andExpect(
                        jsonPath(
                                "$.accounts.overdueInstallments"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.accounts.overdueAmount"
                        ).value(500.00)
                )
                .andExpect(
                        jsonPath(
                                "$.cashFlow.paymentCount"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.cashFlow.collectedAmount"
                        ).value(700.00)
                )
                .andExpect(
                        jsonPath(
                                "$.cashFlow.refundedAmount"
                        ).value(100.00)
                )
                .andExpect(
                        jsonPath(
                                "$.cashFlow.expenseAmount"
                        ).value(200.00)
                )
                .andExpect(
                        jsonPath(
                                "$.cashFlow.instructorPaidAmount"
                        ).value(300.00)
                )
                .andExpect(
                        jsonPath(
                                "$.cashFlow.netCashFlow"
                        ).value(100.00)
                )
                .andExpect(
                        jsonPath(
                                "$.adjustments.discountAmount"
                        ).value(50.00)
                )
                .andExpect(
                        jsonPath(
                                "$.adjustments.chargeAmount"
                        ).value(150.00)
                )
                .andExpect(
                        jsonPath(
                                "$.currentLiabilities.accruedInstructorEarnings"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.currentLiabilities.accruedInstructorAmount"
                        ).value(150.00)
                )
                .andExpect(
                        jsonPath(
                                "$.paymentMethods[0].paymentMethod"
                        ).value("CARD")
                )
                .andExpect(
                        jsonPath(
                                "$.paymentMethods[0].amount"
                        ).value(700.00)
                )
                .andExpect(
                        jsonPath(
                                "$.expenseCategories[0].category"
                        ).value("RENT")
                )
                .andExpect(
                        jsonPath(
                                "$.expenseCategories[0].amount"
                        ).value(200.00)
                );
    }

    @Test
    void accountReportShouldReturnCompleteStatement()
            throws Exception {
        mockMvc.perform(
                        get(
                                REPORTS_URL
                                        + "/accounts/"
                                        + FINANCIAL_ACCOUNT_ID
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.financialAccountId")
                                .value(
                                        FINANCIAL_ACCOUNT_ID
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("OPEN")
                )
                .andExpect(
                        jsonPath("$.totalAmount")
                                .value(1100.00)
                )
                .andExpect(
                        jsonPath("$.paidAmount")
                                .value(600.00)
                )
                .andExpect(
                        jsonPath("$.outstandingAmount")
                                .value(500.00)
                )
                .andExpect(
                        jsonPath(
                                "$.payments.grossCollectedAmount"
                        ).value(700.00)
                )
                .andExpect(
                        jsonPath(
                                "$.payments.refundedAmount"
                        ).value(100.00)
                )
                .andExpect(
                        jsonPath(
                                "$.payments.netCollectedAmount"
                        ).value(600.00)
                )
                .andExpect(
                        jsonPath(
                                "$.installments.totalInstallments"
                        ).value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.installments.paidInstallments"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.installments.overdueInstallments"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.installments.outstandingAmount"
                        ).value(500.00)
                )
                .andExpect(
                        jsonPath(
                                "$.adjustments.discountAmount"
                        ).value(50.00)
                )
                .andExpect(
                        jsonPath(
                                "$.adjustments.chargeAmount"
                        ).value(150.00)
                );
    }

    @Test
    void invalidPeriodShouldReturnBadRequest()
            throws Exception {
        mockMvc.perform(
                        get(REPORTS_URL + "/overview")
                                .param("currency", "EGP")
                                .param(
                                        "fromDate",
                                        "2026-08-01"
                                )
                                .param(
                                        "toDate",
                                        "2026-07-31"
                                )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingAccountShouldReturnNotFound()
            throws Exception {
        mockMvc.perform(
                        get(
                                REPORTS_URL
                                        + "/accounts/"
                                        + UUID.randomUUID()
                        )
                )
                .andExpect(status().isNotFound());
    }
}