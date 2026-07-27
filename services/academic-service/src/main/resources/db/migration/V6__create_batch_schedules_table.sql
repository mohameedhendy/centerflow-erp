CREATE TABLE batch_schedules
(
    id           UUID                     NOT NULL,
    batch_id     UUID                     NOT NULL,
    day_of_week  VARCHAR(15)              NOT NULL,
    start_time   TIME                     NOT NULL,
    end_time     TIME                     NOT NULL,
    active       BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_batch_schedules
        PRIMARY KEY (id),

    CONSTRAINT fk_batch_schedules_batch
        FOREIGN KEY (batch_id)
            REFERENCES batches (id)
            ON DELETE RESTRICT,

    CONSTRAINT uq_batch_schedules_exact_slot
        UNIQUE (
                batch_id,
                day_of_week,
                start_time,
                end_time
            ),

    CONSTRAINT ck_batch_schedules_day
        CHECK (
            day_of_week IN (
                            'MONDAY',
                            'TUESDAY',
                            'WEDNESDAY',
                            'THURSDAY',
                            'FRIDAY',
                            'SATURDAY',
                            'SUNDAY'
                )
            ),

    CONSTRAINT ck_batch_schedules_time
        CHECK (end_time > start_time)
);

CREATE INDEX idx_batch_schedules_batch_id
    ON batch_schedules (batch_id);

CREATE INDEX idx_batch_schedules_day_time
    ON batch_schedules (
                        day_of_week,
                        start_time,
                        end_time
        );

CREATE INDEX idx_batch_schedules_active
    ON batch_schedules (active);