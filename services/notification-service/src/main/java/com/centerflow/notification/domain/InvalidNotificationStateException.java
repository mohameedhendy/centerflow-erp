package com.centerflow.notification.domain;

public class InvalidNotificationStateException
        extends IllegalStateException {

    public InvalidNotificationStateException(
            String message
    ) {
        super(message);
    }
}