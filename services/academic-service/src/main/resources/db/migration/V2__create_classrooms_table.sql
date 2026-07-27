CREATE TABLE classrooms
(
    id          UUID                     NOT NULL,
    branch_id   UUID                     NOT NULL,
    code        VARCHAR(30)              NOT NULL,
    name        VARCHAR(150)             NOT NULL,
    capacity    INTEGER                  NOT NULL,
    floor       VARCHAR(50),
    active      BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_classrooms
        PRIMARY KEY (id),

    CONSTRAINT uq_classrooms_branch_code
        UNIQUE (branch_id, code),

    CONSTRAINT fk_classrooms_branch
        FOREIGN KEY (branch_id)
            REFERENCES branches (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_classrooms_code_not_blank
        CHECK (TRIM(code) <> ''),

    CONSTRAINT ck_classrooms_name_not_blank
        CHECK (TRIM(name) <> ''),

    CONSTRAINT ck_classrooms_capacity
        CHECK (capacity BETWEEN 1 AND 1000)
);

CREATE INDEX idx_classrooms_branch_id
    ON classrooms (branch_id);

CREATE INDEX idx_classrooms_name
    ON classrooms (name);

CREATE INDEX idx_classrooms_capacity
    ON classrooms (capacity);

CREATE INDEX idx_classrooms_active
    ON classrooms (active);