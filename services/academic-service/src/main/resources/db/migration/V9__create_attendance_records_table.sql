CREATE TABLE attendance_records
(
    id              UUID                     NOT NULL,
    session_id      UUID                     NOT NULL,
    enrollment_id   UUID                     NOT NULL,
    student_id      UUID                     NOT NULL,
    status          VARCHAR(20)              NOT NULL,
    notes           VARCHAR(500),
    marked_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_attendance_records
        PRIMARY KEY (id),

    CONSTRAINT fk_attendance_records_session
        FOREIGN KEY (session_id)
            REFERENCES batch_sessions (id)
            ON DELETE RESTRICT,

    CONSTRAINT uq_attendance_session_enrollment
        UNIQUE (session_id, enrollment_id),

    CONSTRAINT uq_attendance_session_student
        UNIQUE (session_id, student_id),

    CONSTRAINT ck_attendance_status
        CHECK (
            status IN (
                       'PRESENT',
                       'ABSENT',
                       'LATE',
                       'EXCUSED'
                )
            )
);

CREATE INDEX idx_attendance_records_session_id
    ON attendance_records (session_id);

CREATE INDEX idx_attendance_records_enrollment_id
    ON attendance_records (enrollment_id);

CREATE INDEX idx_attendance_records_student_id
    ON attendance_records (student_id);

CREATE INDEX idx_attendance_records_session_status
    ON attendance_records (session_id, status);