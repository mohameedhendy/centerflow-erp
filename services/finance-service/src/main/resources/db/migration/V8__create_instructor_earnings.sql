CREATE SEQUENCE instructor_earning_number_sequence
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE instructor_earnings
(
    id                   UUID                     NOT NULL,
    earning_number       VARCHAR(30)              NOT NULL,
    instructor_id        UUID                     NOT NULL,
    session_id           UUID                     NOT NULL,
    batch_id             UUID                     NOT NULL,
    amount               NUMERIC(19, 2)           NOT NULL,
    currency             VARCHAR(3)               NOT NULL,
    session_date         DATE                     NOT NULL,
    description          VARCHAR(500)             NOT NULL,
    status               VARCHAR(20)              NOT NULL,
    payment_method       VARCHAR(30),
    payment_reference    VARCHAR(100),
    cancellation_reason  VARCHAR(500),
    accrued_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    paid_at              TIMESTAMP WITH TIME ZONE,
    cancelled_at         TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    version              BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_instructor_earnings
        PRIMARY KEY (id),

    CONSTRAINT uk_instructor_earnings_number
        UNIQUE (earning_number),

    CONSTRAINT uk_instructor_earnings_session
        UNIQUE (session_id),

    CONSTRAINT uk_instructor_earnings_payment_reference
        UNIQUE (payment_reference),

    CONSTRAINT ck_instructor_earnings_amount
        CHECK (amount > 0),

    CONSTRAINT ck_instructor_earnings_currency
        CHECK (
            CHAR_LENGTH(currency) = 3
            AND UPPER(currency) = currency
        ),

    CONSTRAINT ck_instructor_earnings_description
        CHECK (
            CHAR_LENGTH(TRIM(description)) >= 3
            AND CHAR_LENGTH(description) <= 500
        ),

    CONSTRAINT ck_instructor_earnings_status
        CHECK (
            status IN (
                'ACCRUED',
                'PAID',
                'CANCELLED'
            )
        ),

    CONSTRAINT ck_instructor_earnings_payment_method
        CHECK (
            payment_method IS NULL
            OR payment_method IN (
                'CASH',
                'CARD',
                'BANK_TRANSFER',
                'MOBILE_WALLET'
            )
        ),

    CONSTRAINT ck_instructor_earnings_state
        CHECK (
            (
                status = 'ACCRUED'
                AND payment_method IS NULL
                AND payment_reference IS NULL
                AND paid_at IS NULL
                AND cancellation_reason IS NULL
                AND cancelled_at IS NULL
            )
            OR
            (
                status = 'PAID'
                AND payment_method IS NOT NULL
                AND payment_reference IS NOT NULL
                AND paid_at IS NOT NULL
                AND cancellation_reason IS NULL
                AND cancelled_at IS NULL
            )
            OR
            (
                status = 'CANCELLED'
                AND payment_method IS NULL
                AND payment_reference IS NULL
                AND paid_at IS NULL
                AND cancellation_reason IS NOT NULL
                AND cancelled_at IS NOT NULL
            )
        )
);

CREATE INDEX idx_instructor_earnings_instructor_date
    ON instructor_earnings (
        instructor_id,
        session_date DESC
    );

CREATE INDEX idx_instructor_earnings_batch_date
    ON instructor_earnings (
        batch_id,
        session_date DESC
    );

CREATE INDEX idx_instructor_earnings_status_date
    ON instructor_earnings (
        status,
        session_date DESC
    );