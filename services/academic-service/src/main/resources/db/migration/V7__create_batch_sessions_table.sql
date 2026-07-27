CREATE TABLE batch_sessions
(
    id                  UUID                     NOT NULL,
    batch_id            UUID                     NOT NULL,
    batch_schedule_id   UUID,
    session_date        DATE                     NOT NULL,
    start_time          TIME                     NOT NULL,
    end_time            TIME                     NOT NULL,
    topic               VARCHAR(200),
    status              VARCHAR(20)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_batch_sessions
        PRIMARY KEY (id),

    CONSTRAINT fk_batch_sessions_batch
        FOREIGN KEY (batch_id)
            REFERENCES batches (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_batch_sessions_schedule
        FOREIGN KEY (batch_schedule_id)
            REFERENCES batch_schedules (id)
            ON DELETE RESTRICT,

    CONSTRAINT uq_batch_sessions_exact_slot
        UNIQUE (
                batch_id,
                session_date,
                start_time,
                end_time
            ),

    CONSTRAINT ck_batch_sessions_time
        CHECK (end_time > start_time),

    CONSTRAINT ck_batch_sessions_status
        CHECK (
            status IN (
                       'PLANNED',
                       'COMPLETED',
                       'CANCELLED'
                )
            )
);

CREATE INDEX idx_batch_sessions_batch_id
    ON batch_sessions (batch_id);

CREATE INDEX idx_batch_sessions_schedule_id
    ON batch_sessions (batch_schedule_id);

CREATE INDEX idx_batch_sessions_date
    ON batch_sessions (session_date);

CREATE INDEX idx_batch_sessions_status
    ON batch_sessions (status);