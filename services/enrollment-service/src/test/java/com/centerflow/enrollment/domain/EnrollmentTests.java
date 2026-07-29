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
        assertThat(enrollment.getStudentId()).isEqualTo(studentId);
        assertThat(enrollment.getBatchId()).isEqualTo(batchId);
        assertThat(enrollment.getStatus())
                .isEqualTo(EnrollmentStatus.PENDING_PAYMENT);
    }

    @Test
    void activateShouldChangePendingPaymentToActive() {
        Enrollment enrollment = createEnrollment();

        enrollment.activate();

        assertThat(enrollment.getStatus())
                .isEqualTo(EnrollmentStatus.ACTIVE);
    }

    @Test
    void activateShouldRejectNonPendingEnrollment() {
        Enrollment enrollment = createEnrollment();
        enrollment.activate();

        assertThatThrownBy(enrollment::activate)
                .isInstanceOf(
                        InvalidEnrollmentStatusTransitionException.class
                )
                .hasMessage(
                        "Enrollment status cannot change from "
                                + "ACTIVE to ACTIVE"
                );
    }

    @Test
    void suspendShouldChangeActiveToSuspended() {
        Enrollment enrollment = createEnrollment();
        enrollment.activate();

        enrollment.suspend();

        assertThat(enrollment.getStatus())
                .isEqualTo(EnrollmentStatus.SUSPENDED);
    }

    @Test
    void resumeShouldChangeSuspendedToActive() {
        Enrollment enrollment = createEnrollment();
        enrollment.activate();
        enrollment.suspend();

        enrollment.resume();

        assertThat(enrollment.getStatus())
                .isEqualTo(EnrollmentStatus.ACTIVE);
    }

    @Test
    void completeShouldChangeActiveToCompleted() {
        Enrollment enrollment = createEnrollment();
        enrollment.activate();

        enrollment.complete();

        assertThat(enrollment.getStatus())
                .isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    void suspendShouldRejectPendingPaymentEnrollment() {
        Enrollment enrollment = createEnrollment();

        assertThatThrownBy(enrollment::suspend)
                .isInstanceOf(
                        InvalidEnrollmentStatusTransitionException.class
                )
                .hasMessage(
                        "Enrollment status cannot change from "
                                + "PENDING_PAYMENT to SUSPENDED"
                );
    }

    @Test
    void cancelShouldBeIdempotent() {
        Enrollment enrollment = createEnrollment();

        enrollment.cancel();
        enrollment.cancel();

        assertThat(enrollment.getStatus())
                .isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    void cancelShouldRejectCompletedEnrollment() {
        Enrollment enrollment = createEnrollment();
        enrollment.activate();
        enrollment.complete();

        assertThatThrownBy(enrollment::cancel)
                .isInstanceOf(
                        InvalidEnrollmentStatusTransitionException.class
                )
                .hasMessage(
                        "Enrollment status cannot change from "
                                + "COMPLETED to CANCELLED"
                );
    }

    private Enrollment createEnrollment() {
        return Enrollment.create(
                "ENR-2026-000001",
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }
}