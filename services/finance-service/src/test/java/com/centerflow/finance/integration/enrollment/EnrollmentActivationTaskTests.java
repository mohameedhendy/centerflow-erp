package com.centerflow.finance.integration.enrollment;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentActivationTaskTests {

    @Test
    void taskShouldTrackSuccessfulAttempt() {
        EnrollmentActivationTask task =
                EnrollmentActivationTask.create(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        task.beginAttempt();
        task.markSucceeded();

        assertThat(task.getStatus())
                .isEqualTo(
                        EnrollmentActivationStatus.SUCCEEDED
                );

        assertThat(task.getAttemptCount())
                .isEqualTo(1);

        assertThat(task.getCompletedAt())
                .isNotNull();

        assertThat(task.getLastError())
                .isNull();
    }

    @Test
    void failedTaskShouldAllowAnotherAttempt() {
        EnrollmentActivationTask task =
                EnrollmentActivationTask.create(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        task.beginAttempt();
        task.markFailed(
                "Enrollment Service unavailable"
        );

        assertThat(task.getStatus())
                .isEqualTo(
                        EnrollmentActivationStatus.FAILED
                );

        assertThat(task.getAttemptCount())
                .isEqualTo(1);

        task.beginAttempt();
        task.markSucceeded();

        assertThat(task.getStatus())
                .isEqualTo(
                        EnrollmentActivationStatus.SUCCEEDED
                );

        assertThat(task.getAttemptCount())
                .isEqualTo(2);
    }
}