package com.centerflow.finance.expense;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExpenseApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void expenseApisShouldCreateSearchGetAndCancel()
            throws Exception {
        UUID branchId = UUID.randomUUID();

        String externalReference =
                "API-EXP-" + UUID.randomUUID();

        String requestBody = """
                {
                  "branchId": "%s",
                  "category": "SUPPLIES",
                  "amount": 750.00,
                  "currency": "EGP",
                  "paymentMethod": "CASH",
                  "payee": "Office Supplier",
                  "description": "Printer and office supplies",
                  "expenseDate": "%s",
                  "externalReference": "%s"
                }
                """.formatted(
                branchId,
                LocalDate.now(ZoneOffset.UTC),
                externalReference
        );

        String responseBody = mockMvc.perform(
                        post("/api/v1/finance/expenses")
                                .contentType(
                                        "application/json"
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(
                        jsonPath("$.expenseNumber")
                                .value(startsWith("EXP-"))
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("RECORDED")
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        String expenseId = responseBody
                .replaceAll(
                        ".*\"id\":\"([^\"]+)\".*",
                        "$1"
                );

        mockMvc.perform(
                        get(
                                "/api/v1/finance/expenses/{expenseId}",
                                expenseId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.externalReference")
                                .value(externalReference)
                );

        mockMvc.perform(
                        get("/api/v1/finance/expenses")
                                .param(
                                        "branchId",
                                        branchId.toString()
                                )
                                .param(
                                        "category",
                                        "SUPPLIES"
                                )
                                .param(
                                        "status",
                                        "RECORDED"
                                )
                                .param(
                                        "payee",
                                        "office"
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
                        jsonPath("$.content[0].id")
                                .value(expenseId)
                );

        String cancellationBody = """
                {
                  "reason": "Duplicate expense entry"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/finance/expenses/{expenseId}/cancel",
                                expenseId
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content(cancellationBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("CANCELLED")
                )
                .andExpect(
                        jsonPath("$.cancellationReason")
                                .value(
                                        "Duplicate expense entry"
                                )
                );
    }

    @Test
    void invalidExpenseShouldReturnBadRequest()
            throws Exception {
        String requestBody = """
                {
                  "category": "RENT",
                  "amount": 0,
                  "currency": "EGP",
                  "paymentMethod": "CASH",
                  "payee": "A",
                  "description": "No",
                  "expenseDate": "%s"
                }
                """.formatted(
                LocalDate.now(ZoneOffset.UTC)
                        .plusDays(1)
        );

        mockMvc.perform(
                        post("/api/v1/finance/expenses")
                                .contentType(
                                        "application/json"
                                )
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateReferenceShouldReturnConflict()
            throws Exception {
        String externalReference =
                "DUP-EXP-" + UUID.randomUUID();

        String requestBody = """
                {
                  "category": "OTHER",
                  "amount": 100.00,
                  "currency": "EGP",
                  "paymentMethod": "CASH",
                  "payee": "General Supplier",
                  "description": "General expense entry",
                  "expenseDate": "%s",
                  "externalReference": "%s"
                }
                """.formatted(
                LocalDate.now(ZoneOffset.UTC),
                externalReference
        );

        mockMvc.perform(
                        post("/api/v1/finance/expenses")
                                .contentType(
                                        "application/json"
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/v1/finance/expenses")
                                .contentType(
                                        "application/json"
                                )
                                .content(requestBody)
                )
                .andExpect(status().isConflict());
    }
}