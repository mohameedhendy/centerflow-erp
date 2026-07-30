package com.centerflow.finance.integration.enrollment;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnrollmentActivationProcessorTests {

    @Test
    void processorShouldMarkTaskSuccessful() {
        EnrollmentActivationTaskRepository repository =
                mock(
                        EnrollmentActivationTaskRepository.class
                );

        EnrollmentActivationClient client =
                mock(EnrollmentActivationClient.class);

        EnrollmentActivationTask task =
                EnrollmentActivationTask.create(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        when(repository.findByIdForUpdate(task.getId()))
                .thenReturn(Optional.of(task));

        EnrollmentActivationProcessor processor =
                new EnrollmentActivationProcessor(
                        repository,
                        client
                );

        EnrollmentActivationTaskResponse response =
                processor.process(task.getId());

        verify(client).activateEnrollment(
                task.getEnrollmentId()
        );

        assertThat(response.status())
                .isEqualTo(
                        EnrollmentActivationStatus.SUCCEEDED
                );

        assertThat(response.attemptCount())
                .isEqualTo(1);
    }

    @Test
    void failedTaskShouldSucceedAfterRetry() {
        EnrollmentActivationTaskRepository repository =
                mock(
                        EnrollmentActivationTaskRepository.class
                );

        EnrollmentActivationClient client =
                mock(EnrollmentActivationClient.class);

        EnrollmentActivationTask task =
                EnrollmentActivationTask.create(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        when(repository.findByIdForUpdate(task.getId()))
                .thenReturn(Optional.of(task));

        doThrow(
                new IllegalStateException(
                        "Enrollment Service unavailable"
                )
        )
                .doNothing()
                .when(client)
                .activateEnrollment(
                        task.getEnrollmentId()
                );

        EnrollmentActivationProcessor processor =
                new EnrollmentActivationProcessor(
                        repository,
                        client
                );

        EnrollmentActivationTaskResponse failedResponse =
                processor.process(task.getId());

        assertThat(failedResponse.status())
                .isEqualTo(
                        EnrollmentActivationStatus.FAILED
                );

        assertThat(failedResponse.attemptCount())
                .isEqualTo(1);

        EnrollmentActivationTaskResponse retryResponse =
                processor.process(task.getId());

        assertThat(retryResponse.status())
                .isEqualTo(
                        EnrollmentActivationStatus.SUCCEEDED
                );

        assertThat(retryResponse.attemptCount())
                .isEqualTo(2);
    }
}