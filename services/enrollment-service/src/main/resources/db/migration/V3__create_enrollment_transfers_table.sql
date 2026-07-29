CREATE TABLE enrollment_transfers
(
    id             UUID                     NOT NULL,
    enrollment_id  UUID                     NOT NULL,
    from_batch_id  UUID                     NOT NULL,
    to_batch_id    UUID                     NOT NULL,
    reason         VARCHAR(500)             NOT NULL,
    transferred_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_enrollment_transfers
        PRIMARY KEY (id),

    CONSTRAINT fk_enrollment_transfers_enrollment
        FOREIGN KEY (enrollment_id)
            REFERENCES enrollments (id),

    CONSTRAINT ck_enrollment_transfers_different_batches
        CHECK (from_batch_id <> to_batch_id)
);

CREATE INDEX idx_enrollment_transfers_enrollment
    ON enrollment_transfers (enrollment_id, transferred_at);