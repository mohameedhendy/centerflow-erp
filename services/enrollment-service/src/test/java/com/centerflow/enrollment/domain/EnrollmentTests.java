package com.centerflow.enrollment.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnrollmentTests {

    @Test
    void createShouldInitializePendingPaymentEnrollment() {
        UUID studentId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        Enrollment enrollment = Enrollment.create(
                " enr-2026-000001 ",
                studentId,
                batchId
        );

        assertThat(enrollment.getId()).isNotNull();
        assertThat(enrollment.getEnrollmentNumber())
                .isEqualTo("ENR-2026-000001");
        assertThat(enrollment.getStudentId())
                .isEqualTo(studentId);
        assertThat(enrollment.getBatchId())
                .isEqualTo(batchId);
        assertThat(enrollment.getStatus())
                .isEqualTo(
                        EnrollmentStatus.PENDING_PAYMENT
                );
        assertThat(enrollment.getCreatedAt()).isNotNull();
        assertThat(enrollment.getUpdatedAt()).isNotNull();
    }

    @Test
    void activateShouldChangePendingPaymentToActive() {
        Enrollment enrollment = createEnrollment();

        enrollment.activate();

        assertThat(enrollment.getStatus())
                .isEqualTo(EnrollmentStatus.ACTIVE);
    }

    @Test
    void suspendShouldOnlyBeAllowedForActiveEnrollment() {
        Enrollment enrollment = createEnrollment();

        assertThatThrownBy(enrollment::suspend)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING_PAYMENT")
                .hasMessageContaining("SUSPENDED");
    }

    @Test
    void completeShouldOnlyBeAllowedForActiveEnrollment() {
        Enrollment enrollment = createEnrollment();

        assertThatThrownBy(enrollment::complete)
                .isInstanceOf(IllegalStateException.class);

        enrollment.activate();
        enrollment.complete();

        assertThat(enrollment.getStatus())
                .isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    void cancelShouldBeIdempotent() {
        Enrollment enrollment = createEnrollment();

        enrollment.cancel();
        enrollment.cancel();

        assertThat(enrollment.getStatus())
                .isEqualTo(EnrollmentStatus.CANCELLED);
    }

    private Enrollment createEnrollment() {
        return Enrollment.create(
                "ENR-2026-000001",
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }
}