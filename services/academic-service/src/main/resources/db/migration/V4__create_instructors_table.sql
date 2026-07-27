CREATE TABLE instructors
(
    id              UUID                     NOT NULL,
    code            VARCHAR(30)              NOT NULL,
    first_name      VARCHAR(100)             NOT NULL,
    last_name       VARCHAR(100)             NOT NULL,
    email           VARCHAR(320),
    phone           VARCHAR(30),
    specialization  VARCHAR(150),
    bio             VARCHAR(1000),
    active          BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_instructors
        PRIMARY KEY (id),

    CONSTRAINT uq_instructors_code
        UNIQUE (code),

    CONSTRAINT uq_instructors_email
        UNIQUE (email),

    CONSTRAINT ck_instructors_code_not_blank
        CHECK (TRIM(code) <> ''),

    CONSTRAINT ck_instructors_first_name_not_blank
        CHECK (TRIM(first_name) <> ''),

    CONSTRAINT ck_instructors_last_name_not_blank
        CHECK (TRIM(last_name) <> ''),

    CONSTRAINT ck_instructors_email_not_blank
        CHECK (
            email IS NULL
                OR TRIM(email) <> ''
            )
);

CREATE INDEX idx_instructors_name
    ON instructors (last_name, first_name);

CREATE INDEX idx_instructors_specialization
    ON instructors (specialization);

CREATE INDEX idx_instructors_active
    ON instructors (active);