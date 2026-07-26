CREATE TABLE users
(
    id             UUID                     NOT NULL,
    email          VARCHAR(320)             NOT NULL,
    password_hash  VARCHAR(255)             NOT NULL,
    status         VARCHAR(30)              NOT NULL,
    email_verified BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (
        status IN (
                   'PENDING_VERIFICATION',
                   'ACTIVE',
                   'LOCKED',
                   'DISABLED'
            )
        )
);

CREATE TABLE roles
(
    id          UUID                     NOT NULL,
    name        VARCHAR(50)              NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE permissions
(
    id          UUID                     NOT NULL,
    name        VARCHAR(100)             NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uq_permissions_name UNIQUE (name)
);

CREATE TABLE user_roles
(
    user_id     UUID                     NOT NULL,
    role_id     UUID                     NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    assigned_by UUID,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
            REFERENCES roles (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_user_roles_assigned_by
        FOREIGN KEY (assigned_by)
            REFERENCES users (id)
            ON DELETE SET NULL
);

CREATE TABLE role_permissions
(
    role_id      UUID                     NOT NULL,
    permission_id UUID                    NOT NULL,
    assigned_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    assigned_by  UUID,

    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
            REFERENCES roles (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
            REFERENCES permissions (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_role_permissions_assigned_by
        FOREIGN KEY (assigned_by)
            REFERENCES users (id)
            ON DELETE SET NULL
);

CREATE TABLE refresh_token_sessions
(
    id         UUID                     NOT NULL,
    user_id    UUID                     NOT NULL,
    token_hash VARCHAR(128)             NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_refresh_token_sessions PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token_sessions_token_hash UNIQUE (token_hash),

    CONSTRAINT fk_refresh_token_sessions_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_refresh_token_sessions_expiry
        CHECK (expires_at > created_at)
);

CREATE TABLE password_reset_tokens
(
    id         UUID                     NOT NULL,
    user_id    UUID                     NOT NULL,
    token_hash VARCHAR(128)             NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at    TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_tokens_token_hash UNIQUE (token_hash),

    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_password_reset_tokens_expiry
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_user_roles_role_id
    ON user_roles (role_id);

CREATE INDEX idx_role_permissions_permission_id
    ON role_permissions (permission_id);

CREATE INDEX idx_refresh_token_sessions_user_id
    ON refresh_token_sessions (user_id);

CREATE INDEX idx_refresh_token_sessions_expires_at
    ON refresh_token_sessions (expires_at);

CREATE INDEX idx_password_reset_tokens_user_id
    ON password_reset_tokens (user_id);

CREATE INDEX idx_password_reset_tokens_expires_at
    ON password_reset_tokens (expires_at);