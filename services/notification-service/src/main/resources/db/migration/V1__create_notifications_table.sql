CREATE TABLE notifications
(
    id                UUID                     NOT NULL,
    recipient_user_id UUID                     NOT NULL,
    type              VARCHAR(50)              NOT NULL,
    title             VARCHAR(150)             NOT NULL,
    message           VARCHAR(1000)            NOT NULL,
    reference_type    VARCHAR(50),
    reference_id      UUID,
    source_event_id   UUID,
    status            VARCHAR(20)              NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at           TIMESTAMP WITH TIME ZONE,
    archived_at       TIMESTAMP WITH TIME ZONE,
    version           BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_notifications
        PRIMARY KEY (id),

    CONSTRAINT uk_notifications_source_event
        UNIQUE (source_event_id),

    CONSTRAINT ck_notifications_type
        CHECK (
            CHAR_LENGTH(TRIM(type)) > 0
        ),

    CONSTRAINT ck_notifications_title
        CHECK (
            CHAR_LENGTH(TRIM(title)) > 0
        ),

    CONSTRAINT ck_notifications_message
        CHECK (
            CHAR_LENGTH(TRIM(message)) > 0
        ),

    CONSTRAINT ck_notifications_reference
        CHECK (
            (
                reference_type IS NULL
                AND reference_id IS NULL
            )
            OR
            (
                reference_type IS NOT NULL
                AND reference_id IS NOT NULL
            )
        ),

    CONSTRAINT ck_notifications_status
        CHECK (
            status IN (
                'UNREAD',
                'READ',
                'ARCHIVED'
            )
        )
);

CREATE INDEX idx_notifications_recipient_status_created
    ON notifications (
        recipient_user_id,
        status,
        created_at DESC
    );

CREATE INDEX idx_notifications_recipient_created
    ON notifications (
        recipient_user_id,
        created_at DESC
    );

CREATE INDEX idx_notifications_reference
    ON notifications (
        reference_type,
        reference_id
    );