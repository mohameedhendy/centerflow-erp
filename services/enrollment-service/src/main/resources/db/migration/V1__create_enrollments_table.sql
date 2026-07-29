CREATE TABLE enrollments
(
    id                UUID                     NOT NULL,
    enrollment_number VARCHAR(30)              NOT NULL,
    student_id        UUID                     NOT NULL,
    batch_id          UUID                     NOT NULL,
    status            VARCHAR(30)              NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    version           BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_enrollments
        PRIMARY KEY (id),

    CONSTRAINT uk_enrollments_number
        UNIQUE (enrollment_number),

    CONSTRAINT ck_enrollments_status
        CHECK (
            status IN (
                       'PENDING_PAYMENT',
                       'ACTIVE',
                       'SUSPENDED',
                       'COMPLETED',
                       'CANCELLED'
                )
            )
);

CREATE INDEX idx_enrollments_student_id
    ON enrollments (student_id);

CREATE INDEX idx_enrollments_batch_id
    ON enrollments (batch_id);

CREATE INDEX idx_enrollments_status
    ON enrollments (status);

CREATE INDEX idx_enrollments_created_at
    ON enrollments (created_at);