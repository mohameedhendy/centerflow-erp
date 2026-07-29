package com.centerflow.enrollment.api;

import com.centerflow.enrollment.domain.Enrollment;
import com.centerflow.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EnrollmentSearchApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    void searchShouldReturnPaginatedEnrollments()
            throws Exception {
        saveEnrollment(
                "ENR-2026-001001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                false
        );

        saveEnrollment(
                "ENR-2026-001002",
                UUID.randomUUID(),
                UUID.randomUUID(),
                false
        );

        saveEnrollment(
                "ENR-2026-001003",
                UUID.randomUUID(),
                UUID.randomUUID(),
                false
        );

        mockMvc.perform(
                        get("/api/v1/enrollments")
                                .param("page", "0")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content", hasSize(2))
                )
                .andExpect(
                        jsonPath("$.page").value(0)
                )
                .andExpect(
                        jsonPath("$.size").value(2)
                )
                .andExpect(
                        jsonPath("$.totalElements").value(3)
                )
                .andExpect(
                        jsonPath("$.totalPages").value(2)
                )
                .andExpect(
                        jsonPath("$.first").value(true)
                )
                .andExpect(
                        jsonPath("$.last").value(false)
                );
    }

    @Test
    void searchShouldFilterByPartialEnrollmentNumber()
            throws Exception {
        saveEnrollment(
                "ENR-2026-002345",
                UUID.randomUUID(),
                UUID.randomUUID(),
                false
        );

        saveEnrollment(
                "ENR-2026-009999",
                UUID.randomUUID(),
                UUID.randomUUID(),
                false
        );

        mockMvc.perform(
                        get("/api/v1/enrollments")
                                .param(
                                        "enrollmentNumber",
                                        "2345"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content", hasSize(1))
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].enrollmentNumber"
                        ).value("ENR-2026-002345")
                )
                .andExpect(
                        jsonPath("$.totalElements").value(1)
                );
    }

    @Test
    void searchShouldIgnoreEnrollmentNumberCase()
            throws Exception {
        saveEnrollment(
                "ENR-2026-003456",
                UUID.randomUUID(),
                UUID.randomUUID(),
                false
        );

        mockMvc.perform(
                        get("/api/v1/enrollments")
                                .param(
                                        "enrollmentNumber",
                                        "enr-2026-003456"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content", hasSize(1))
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].enrollmentNumber"
                        ).value("ENR-2026-003456")
                );
    }

    @Test
    void searchShouldFilterByStudentId()
            throws Exception {
        UUID targetStudentId = UUID.randomUUID();

        saveEnrollment(
                "ENR-2026-004001",
                targetStudentId,
                UUID.randomUUID(),
                false
        );

        saveEnrollment(
                "ENR-2026-004002",
                UUID.randomUUID(),
                UUID.randomUUID(),
                false
        );

        mockMvc.perform(
                        get("/api/v1/enrollments")
                                .param(
                                        "studentId",
                                        targetStudentId.toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content", hasSize(1))
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].studentId"
                        ).value(targetStudentId.toString())
                );
    }

    @Test
    void searchShouldFilterByBatchAndStatus()
            throws Exception {
        UUID targetBatchId = UUID.randomUUID();

        saveEnrollment(
                "ENR-2026-005001",
                UUID.randomUUID(),
                targetBatchId,
                true
        );

        saveEnrollment(
                "ENR-2026-005002",
                UUID.randomUUID(),
                targetBatchId,
                false
        );

        saveEnrollment(
                "ENR-2026-005003",
                UUID.randomUUID(),
                UUID.randomUUID(),
                true
        );

        mockMvc.perform(
                        get("/api/v1/enrollments")
                                .param(
                                        "batchId",
                                        targetBatchId.toString()
                                )
                                .param("status", "ACTIVE")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content", hasSize(1))
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].enrollmentNumber"
                        ).value("ENR-2026-005001")
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].batchId"
                        ).value(targetBatchId.toString())
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].status"
                        ).value("ACTIVE")
                );
    }

    @Test
    void searchShouldReturn400ForNegativePage()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/enrollments")
                                .param("page", "-1")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Page index must be zero "
                                                + "or greater"
                                )
                );
    }

    @Test
    void searchShouldReturn400ForInvalidPageSize()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/enrollments")
                                .param("size", "101")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Page size must be "
                                                + "between 1 and 100"
                                )
                );
    }

    @Test
    void searchShouldReturn400ForInvalidStatus()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/enrollments")
                                .param(
                                        "status",
                                        "UNKNOWN_STATUS"
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid value for parameter: "
                                                + "status"
                                )
                );
    }

    @Test
    void searchShouldReturn400ForInvalidStudentId()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/enrollments")
                                .param(
                                        "studentId",
                                        "not-a-valid-uuid"
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid value for parameter: "
                                                + "studentId"
                                )
                );
    }

    private Enrollment saveEnrollment(
            String enrollmentNumber,
            UUID studentId,
            UUID batchId,
            boolean active
    ) {
        Enrollment enrollment = Enrollment.create(
                enrollmentNumber,
                studentId,
                batchId
        );

        if (active) {
            enrollment.activate();
        }

        return enrollmentRepository.saveAndFlush(enrollment);
    }
}