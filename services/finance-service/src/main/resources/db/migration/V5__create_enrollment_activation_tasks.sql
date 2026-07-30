CREATE TABLE enrollment_activation_tasks
(
    id              UUID                     NOT NULL,
    enrollment_id   UUID                     NOT NULL,
    payment_id      UUID                     NOT NULL,
    status          VARCHAR(20)              NOT NULL,
    attempt_count   INTEGER                  NOT NULL DEFAULT 0,
    last_error      VARCHAR(1000),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at    TIMESTAMP WITH TIME ZONE,
    version         BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_enrollment_activation_tasks
        PRIMARY KEY (id),

    CONSTRAINT uk_activation_tasks_enrollment
        UNIQUE (enrollment_id),

    CONSTRAINT fk_activation_tasks_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments (id),

    CONSTRAINT ck_activation_tasks_status
        CHECK (
            status IN (
                'PENDING',
                'SUCCEEDED',
                'FAILED'
            )
        ),

    CONSTRAINT ck_activation_tasks_attempt_count
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_activation_tasks_status_updated
    ON enrollment_activation_tasks (
        status,
        updated_at
    );