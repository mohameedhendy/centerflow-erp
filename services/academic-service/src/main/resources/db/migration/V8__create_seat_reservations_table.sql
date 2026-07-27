CREATE TABLE seat_reservations
(
    id             UUID                     NOT NULL,
    batch_id       UUID                     NOT NULL,
    enrollment_id  UUID                     NOT NULL,
    status         VARCHAR(20)              NOT NULL,
    reserved_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    released_at    TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_seat_reservations
        PRIMARY KEY (id),

    CONSTRAINT fk_seat_reservations_batch
        FOREIGN KEY (batch_id)
            REFERENCES batches (id)
            ON DELETE RESTRICT,

    CONSTRAINT uq_seat_reservations_batch_enrollment
        UNIQUE (batch_id, enrollment_id),

    CONSTRAINT ck_seat_reservations_status
        CHECK (
            status IN (
                       'RESERVED',
                       'RELEASED'
                )
            ),

    CONSTRAINT ck_seat_reservations_release_time
        CHECK (
            status = 'RESERVED'
                OR released_at IS NOT NULL
            )
);

CREATE INDEX idx_seat_reservations_batch_id
    ON seat_reservations (batch_id);

CREATE INDEX idx_seat_reservations_enrollment_id
    ON seat_reservations (enrollment_id);

CREATE INDEX idx_seat_reservations_batch_status
    ON seat_reservations (batch_id, status);