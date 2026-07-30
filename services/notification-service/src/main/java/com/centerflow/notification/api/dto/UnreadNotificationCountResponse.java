package com.centerflow.notification.api.dto;

import java.util.UUID;

public record UnreadNotificationCountResponse(

        UUID recipientUserId,
        long unreadCount

) {
}