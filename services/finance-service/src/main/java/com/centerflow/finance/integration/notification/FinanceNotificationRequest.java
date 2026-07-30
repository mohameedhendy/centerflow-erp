package com.centerflow.finance.integration.notification;

import java.util.UUID;

public record FinanceNotificationRequest(

        UUID recipientUserId,
        FinanceNotificationType type,
        String title,
        String message,
        String referenceType,
        UUID referenceId,
        UUID sourceEventId

) {

    public static FinanceNotificationRequest from(
            FinanceNotificationEvent event
    ) {
        return new FinanceNotificationRequest(
                event.recipientUserId(),
                event.type(),
                event.title(),
                event.message(),
                event.referenceType(),
                event.referenceId(),
                event.sourceEventId()
        );
    }
}