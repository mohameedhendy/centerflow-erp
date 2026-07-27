CREATE TABLE batches
(
    id               UUID                     NOT NULL,
    code             VARCHAR(30)              NOT NULL,
    name             VARCHAR(150)             NOT NULL,
    branch_id        UUID                     NOT NULL,
    classroom_id     UUID                     NOT NULL,
    course_level_id  UUID                     NOT NULL,
    instructor_id    UUID                     NOT NULL,
    capacity         INTEGER                  NOT NULL,
    start_date       DATE                     NOT NULL,
    end_date         DATE                     NOT NULL,
    status           VARCHAR(30)              NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_batches
        PRIMARY KEY (id),

    CONSTRAINT uq_batches_code
        UNIQUE (code),

    CONSTRAINT fk_batches_branch
        FOREIGN KEY (branch_id)
            REFERENCES branches (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_batches_classroom
        FOREIGN KEY (classroom_id)
            REFERENCES classrooms (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_batches_course_level
        FOREIGN KEY (course_level_id)
            REFERENCES course_levels (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_batches_instructor
        FOREIGN KEY (instructor_id)
            REFERENCES instructors (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_batches_code_not_blank
        CHECK (TRIM(code) <> ''),

    CONSTRAINT ck_batches_name_not_blank
        CHECK (TRIM(name) <> ''),

    CONSTRAINT ck_batches_capacity
        CHECK (capacity BETWEEN 1 AND 1000),

    CONSTRAINT ck_batches_period
        CHECK (end_date >= start_date),

    CONSTRAINT ck_batches_status
        CHECK (
            status IN (
                       'DRAFT',
                       'OPEN_FOR_ENROLLMENT',
                       'IN_PROGRESS',
                       'COMPLETED',
                       'CANCELLED'
                )
            )
);

CREATE INDEX idx_batches_branch_id
    ON batches (branch_id);

CREATE INDEX idx_batches_classroom_id
    ON batches (classroom_id);

CREATE INDEX idx_batches_course_level_id
    ON batches (course_level_id);

CREATE INDEX idx_batches_instructor_id
    ON batches (instructor_id);

CREATE INDEX idx_batches_start_date
    ON batches (start_date);

CREATE INDEX idx_batches_end_date
    ON batches (end_date);

CREATE INDEX idx_batches_status
    ON batches (status);