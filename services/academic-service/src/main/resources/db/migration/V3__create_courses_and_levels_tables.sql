CREATE TABLE courses
(
    id           UUID                     NOT NULL,
    code         VARCHAR(30)              NOT NULL,
    name         VARCHAR(150)             NOT NULL,
    description  VARCHAR(1000),
    active       BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_courses
        PRIMARY KEY (id),

    CONSTRAINT uq_courses_code
        UNIQUE (code),

    CONSTRAINT ck_courses_code_not_blank
        CHECK (TRIM(code) <> ''),

    CONSTRAINT ck_courses_name_not_blank
        CHECK (TRIM(name) <> '')
);

CREATE INDEX idx_courses_name
    ON courses (name);

CREATE INDEX idx_courses_active
    ON courses (active);


CREATE TABLE course_levels
(
    id               UUID                     NOT NULL,
    course_id        UUID                     NOT NULL,
    code             VARCHAR(30)              NOT NULL,
    name             VARCHAR(150)             NOT NULL,
    sequence_number  INTEGER                  NOT NULL,
    duration_hours   INTEGER                  NOT NULL,
    description      VARCHAR(1000),
    active           BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_course_levels
        PRIMARY KEY (id),

    CONSTRAINT fk_course_levels_course
        FOREIGN KEY (course_id)
            REFERENCES courses (id)
            ON DELETE RESTRICT,

    CONSTRAINT uq_course_levels_course_code
        UNIQUE (course_id, code),

    CONSTRAINT uq_course_levels_course_sequence
        UNIQUE (course_id, sequence_number),

    CONSTRAINT ck_course_levels_code_not_blank
        CHECK (TRIM(code) <> ''),

    CONSTRAINT ck_course_levels_name_not_blank
        CHECK (TRIM(name) <> ''),

    CONSTRAINT ck_course_levels_sequence
        CHECK (sequence_number BETWEEN 1 AND 100),

    CONSTRAINT ck_course_levels_duration
        CHECK (duration_hours BETWEEN 1 AND 2000)
);

CREATE INDEX idx_course_levels_course_id
    ON course_levels (course_id);

CREATE INDEX idx_course_levels_name
    ON course_levels (name);

CREATE INDEX idx_course_levels_active
    ON course_levels (active);