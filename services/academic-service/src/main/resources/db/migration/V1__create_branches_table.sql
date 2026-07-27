CREATE TABLE branches
(
    id           UUID                     NOT NULL,
    code         VARCHAR(30)              NOT NULL,
    name         VARCHAR(150)             NOT NULL,
    phone        VARCHAR(30),
    email        VARCHAR(320),
    address      VARCHAR(500),
    city         VARCHAR(100),
    active       BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_branches
        PRIMARY KEY (id),

    CONSTRAINT uq_branches_code
        UNIQUE (code),

    CONSTRAINT ck_branches_code_not_blank
        CHECK (TRIM(code) <> ''),

    CONSTRAINT ck_branches_name_not_blank
        CHECK (TRIM(name) <> '')
);

CREATE INDEX idx_branches_name
    ON branches (name);

CREATE INDEX idx_branches_city
    ON branches (city);

CREATE INDEX idx_branches_active
    ON branches (active);