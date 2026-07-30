package com.centerflow.notification.api.dto;

import com.centerflow.notification.domain.Notification;
import com.centerflow.notification.domain.NotificationStatus;
import com.centerflow.notification.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(

        UUID id,
        UUID recipientUserId,
        NotificationType type,
        String title,
        String message,
        String referenceType,
        UUID referenceId,
        UUID sourceEventId,
        NotificationStatus status,
        Instant createdAt,
        Instant readAt,
        Instant archivedAt

) {

    public static NotificationResponse from(
            Notification notification
    ) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipientUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getSourceEventId(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getArchivedAt()
        );
    }
}