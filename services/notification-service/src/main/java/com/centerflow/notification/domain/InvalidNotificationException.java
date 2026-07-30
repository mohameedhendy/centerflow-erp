package com.centerflow.notification.domain;

public class InvalidNotificationException
        extends IllegalArgumentException {

    public InvalidNotificationException(String message) {
        super(message);
    }
}