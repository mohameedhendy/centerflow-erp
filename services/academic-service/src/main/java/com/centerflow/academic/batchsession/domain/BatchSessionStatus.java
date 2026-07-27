package com.centerflow.academic.batchsession.domain;

public enum BatchSessionStatus {

    PLANNED,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(
            BatchSessionStatus targetStatus
    ) {
        if (this == targetStatus) {
            return true;
        }

        return switch (this) {
            case PLANNED ->
                    targetStatus == COMPLETED
                            || targetStatus == CANCELLED;

            case CANCELLED ->
                    targetStatus == PLANNED;

            case COMPLETED -> false;
        };
    }

    public boolean canBeEdited() {
        return this != COMPLETED;
    }
}