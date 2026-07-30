package com.centerflow.finance.integration.enrollment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EnrollmentActivationEventListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    EnrollmentActivationEventListener.class
            );

    private final EnrollmentActivationProcessor processor;

    public EnrollmentActivationEventListener(
            EnrollmentActivationProcessor processor
    ) {
        this.processor = processor;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            EnrollmentActivationRequested event
    ) {
        try {
            EnrollmentActivationTaskResponse response =
                    processor.process(event.taskId());

            if (
                    response.status()
                            == EnrollmentActivationStatus.FAILED
            ) {
                LOGGER.warn(
                        "Enrollment activation task {} failed: {}",
                        response.id(),
                        response.lastError()
                );
            }
        }
        catch (RuntimeException exception) {
            LOGGER.error(
                    "Could not process enrollment activation task {}",
                    event.taskId(),
                    exception
            );
        }
    }
}