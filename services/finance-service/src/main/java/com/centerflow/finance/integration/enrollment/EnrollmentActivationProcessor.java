package com.centerflow.finance.integration.enrollment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EnrollmentActivationProcessor {

    private final EnrollmentActivationTaskRepository
            taskRepository;

    private final EnrollmentActivationClient
            activationClient;

    public EnrollmentActivationProcessor(
            EnrollmentActivationTaskRepository
                    taskRepository,
            EnrollmentActivationClient activationClient
    ) {
        this.taskRepository = taskRepository;
        this.activationClient = activationClient;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public EnrollmentActivationTaskResponse process(
            UUID taskId
    ) {
        EnrollmentActivationTask task =
                taskRepository
                        .findByIdForUpdate(taskId)
                        .orElseThrow(
                                () ->
                                        new EnrollmentActivationTaskNotFoundException(
                                                taskId
                                        )
                        );

        if (
                task.getStatus()
                        == EnrollmentActivationStatus.SUCCEEDED
        ) {
            return EnrollmentActivationTaskResponse.from(
                    task
            );
        }

        task.beginAttempt();

        try {
            activationClient.activateEnrollment(
                    task.getEnrollmentId()
            );

            task.markSucceeded();
        }
        catch (RuntimeException exception) {
            task.markFailed(
                    resolveErrorMessage(exception)
            );
        }

        return EnrollmentActivationTaskResponse.from(
                task
        );
    }

    @Transactional(readOnly = true)
    public EnrollmentActivationTaskResponse
    getByEnrollmentId(
            UUID enrollmentId
    ) {
        EnrollmentActivationTask task =
                taskRepository
                        .findByEnrollmentId(enrollmentId)
                        .orElseThrow(
                                () ->
                                        EnrollmentActivationTaskNotFoundException
                                                .forEnrollment(
                                                        enrollmentId
                                                )
                        );

        return EnrollmentActivationTaskResponse.from(
                task
        );
    }

    private String resolveErrorMessage(
            RuntimeException exception
    ) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception
                    .getClass()
                    .getSimpleName();
        }

        return message;
    }
}