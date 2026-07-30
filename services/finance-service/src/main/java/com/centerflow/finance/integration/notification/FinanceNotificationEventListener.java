package com.centerflow.finance.integration.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FinanceNotificationEventListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    FinanceNotificationEventListener.class
            );

    private final FinanceNotificationClient
            notificationClient;

    public FinanceNotificationEventListener(
            FinanceNotificationClient notificationClient
    ) {
        this.notificationClient = notificationClient;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            FinanceNotificationEvent event
    ) {
        try {
            notificationClient.createNotification(event);
        }
        catch (RuntimeException exception) {
            LOGGER.warn(
                    "Could not create notification for "
                            + "finance event {} with source ID {}",
                    event.type(),
                    event.sourceEventId(),
                    exception
            );
        }
    }
}