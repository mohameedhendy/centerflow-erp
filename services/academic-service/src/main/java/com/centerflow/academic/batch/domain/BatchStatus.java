package com.centerflow.academic.batch.domain;

public enum BatchStatus {

    DRAFT,
    OPEN_FOR_ENROLLMENT,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(
            BatchStatus targetStatus
    ) {
        if (this == targetStatus) {
            return true;
        }

        return switch (this) {
            case DRAFT ->
                    targetStatus == OPEN_FOR_ENROLLMENT
                            || targetStatus == CANCELLED;

            case OPEN_FOR_ENROLLMENT ->
                    targetStatus == IN_PROGRESS
                            || targetStatus == CANCELLED;

            case IN_PROGRESS ->
                    targetStatus == COMPLETED
                            || targetStatus == CANCELLED;

            case COMPLETED, CANCELLED -> false;
        };
    }

    public boolean allowsConfigurationChanges() {
        return this == DRAFT
                || this == OPEN_FOR_ENROLLMENT;
    }
}