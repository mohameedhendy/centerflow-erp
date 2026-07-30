package com.centerflow.notification.api.dto;

public record NotificationCreationResponse(

        NotificationResponse notification,
        boolean created

) {
}