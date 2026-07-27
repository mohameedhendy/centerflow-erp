package com.centerflow.academic.common.exception;

public class SessionConfigurationLockedException
        extends RuntimeException {

    public SessionConfigurationLockedException() {
        super(
                "Completed session configuration cannot be changed"
        );
    }
}