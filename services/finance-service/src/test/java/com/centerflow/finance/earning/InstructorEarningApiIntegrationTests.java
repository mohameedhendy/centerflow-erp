package com.centerflow.finance.earning;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
class InstructorEarningApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;


    @Test
    void earningApisShouldRecordSearchGetAndPay()
            throws Exception {
        UUID instructorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        String requestBody = """
                {
                  "instructorId": "%s",
                  "sessionId": "%s",
                  "batchId": "%s",
                  "amount": 400.00,
                  "currency": "EGP",
                  "sessionDate": "%s",
                  "description": "Completed backend session"
                }
                """.formatted(
                instructorId,
                sessionId,
                batchId,
                LocalDate.now(ZoneOffset.UTC)
        );

        MvcResult createResult = mockMvc.perform(
                        post(
                                "/api/v1/finance/instructor-earnings"
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(
                        jsonPath("$.earningNumber")
                                .value(startsWith("ERN-"))
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACCRUED")
                )
                .andReturn();

        String location =
                createResult
                        .getResponse()
                        .getHeader("Location");

        if (location == null || location.isBlank()) {
            throw new IllegalStateException(
                    "Created earning Location header is missing"
            );
        }

        String earningId =
                location.substring(
                        location.lastIndexOf('/') + 1
                );

        mockMvc.perform(
                        get(
                                "/api/v1/finance/instructor-earnings/{earningId}",
                                earningId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.sessionId")
                                .value(sessionId.toString())
                );

        LocalDate currentDate =
                LocalDate.now(ZoneOffset.UTC);

        mockMvc.perform(
                        get(
                                "/api/v1/finance/instructor-earnings"
                        )
                                .param(
                                        "instructorId",
                                        instructorId.toString()
                                )
                                .param(
                                        "batchId",
                                        batchId.toString()
                                )
                                .param(
                                        "status",
                                        "ACCRUED"
                                )
                                .param(
                                        "fromDate",
                                        currentDate.toString()
                                )
                                .param(
                                        "toDate",
                                        currentDate.toString()
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
                                .value(earningId)
                );

        String paymentReference =
                "API-PAY-" + UUID.randomUUID();

        String paymentBody = """
                {
                  "paymentMethod": "BANK_TRANSFER",
                  "paymentReference": "%s"
                }
                """.formatted(paymentReference);

        mockMvc.perform(
                        post(
                                "/api/v1/finance/instructor-earnings/{earningId}/pay",
                                earningId
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content(paymentBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("PAID")
                )
                .andExpect(
                        jsonPath("$.paymentReference")
                                .value(paymentReference)
                )
                .andExpect(
                        jsonPath("$.paidAt")
                                .isNotEmpty()
                );
    }

    @Test
    void duplicateSessionWithDifferentDataShouldReturnConflict()
            throws Exception {
        UUID sessionId = UUID.randomUUID();

        String firstBody = requestBody(
                sessionId,
                "300.00"
        );

        String secondBody = requestBody(
                sessionId,
                "500.00"
        );

        mockMvc.perform(
                        post(
                                "/api/v1/finance/instructor-earnings"
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content(firstBody)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post(
                                "/api/v1/finance/instructor-earnings"
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content(secondBody)
                )
                .andExpect(status().isConflict());
    }

    @Test
    void invalidEarningShouldReturnBadRequest()
            throws Exception {
        String requestBody = """
                {
                  "instructorId": "%s",
                  "sessionId": "%s",
                  "batchId": "%s",
                  "amount": 0,
                  "currency": "EGP",
                  "sessionDate": "%s",
                  "description": "No"
                }
                """.formatted(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now(ZoneOffset.UTC)
                        .plusDays(1)
        );

        mockMvc.perform(
                        post(
                                "/api/v1/finance/instructor-earnings"
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }

    private String requestBody(
            UUID sessionId,
            String amount
    ) {
        return """
                {
                  "instructorId": "%s",
                  "sessionId": "%s",
                  "batchId": "%s",
                  "amount": %s,
                  "currency": "EGP",
                  "sessionDate": "%s",
                  "description": "Completed instructor session"
                }
                """.formatted(
                UUID.randomUUID(),
                sessionId,
                UUID.randomUUID(),
                amount,
                LocalDate.now(ZoneOffset.UTC)
        );
    }
}