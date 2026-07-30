package com.centerflow.notification.api.dto;

import com.centerflow.notification.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateNotificationRequest(

        @NotNull(message = "Recipient user ID is required")
        UUID recipientUserId,

        @NotNull(message = "Notification type is required")
        NotificationType type,

        @NotBlank(message = "Notification title is required")
        @Size(
                max = 150,
                message = "Notification title must not exceed 150 characters"
        )
        String title,

        @NotBlank(message = "Notification message is required")
        @Size(
                max = 1000,
                message = "Notification message must not exceed 1000 characters"
        )
        String message,

        @Size(
                max = 50,
                message = "Reference type must not exceed 50 characters"
        )
        String referenceType,

        UUID referenceId,

        UUID sourceEventId

) {
}