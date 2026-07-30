CREATE SEQUENCE expense_number_sequence
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE expenses
(
    id                   UUID                     NOT NULL,
    expense_number       VARCHAR(30)              NOT NULL,
    branch_id            UUID,
    category             VARCHAR(30)              NOT NULL,
    amount               NUMERIC(19, 2)           NOT NULL,
    currency             VARCHAR(3)               NOT NULL,
    payment_method       VARCHAR(30)              NOT NULL,
    payee                VARCHAR(150)             NOT NULL,
    description          VARCHAR(500)             NOT NULL,
    expense_date         DATE                     NOT NULL,
    external_reference   VARCHAR(100),
    status               VARCHAR(20)              NOT NULL,
    cancellation_reason  VARCHAR(500),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    cancelled_at         TIMESTAMP WITH TIME ZONE,
    version              BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_expenses
        PRIMARY KEY (id),

    CONSTRAINT uk_expenses_number
        UNIQUE (expense_number),

    CONSTRAINT uk_expenses_external_reference
        UNIQUE (external_reference),

    CONSTRAINT ck_expenses_category
        CHECK (
            category IN (
                'RENT',
                'UTILITIES',
                'SUPPLIES',
                'MAINTENANCE',
                'MARKETING',
                'TRANSPORTATION',
                'SALARIES',
                'TAXES',
                'OTHER'
            )
        ),

    CONSTRAINT ck_expenses_amount
        CHECK (amount > 0),

    CONSTRAINT ck_expenses_currency
        CHECK (
            CHAR_LENGTH(currency) = 3
            AND UPPER(currency) = currency
        ),

    CONSTRAINT ck_expenses_payment_method
        CHECK (
            payment_method IN (
                'CASH',
                'CARD',
                'BANK_TRANSFER',
                'MOBILE_WALLET'
            )
        ),

    CONSTRAINT ck_expenses_payee
        CHECK (
            CHAR_LENGTH(TRIM(payee)) >= 2
            AND CHAR_LENGTH(payee) <= 150
        ),

    CONSTRAINT ck_expenses_description
        CHECK (
            CHAR_LENGTH(TRIM(description)) >= 3
            AND CHAR_LENGTH(description) <= 500
        ),

    CONSTRAINT ck_expenses_external_reference
        CHECK (
            external_reference IS NULL
            OR CHAR_LENGTH(TRIM(external_reference)) > 0
        ),

    CONSTRAINT ck_expenses_status
        CHECK (
            status IN (
                'RECORDED',
                'CANCELLED'
            )
        ),

    CONSTRAINT ck_expenses_cancellation
        CHECK (
            (
                status = 'RECORDED'
                AND cancellation_reason IS NULL
                AND cancelled_at IS NULL
            )
            OR
            (
                status = 'CANCELLED'
                AND cancellation_reason IS NOT NULL
                AND cancelled_at IS NOT NULL
            )
        )
);

CREATE INDEX idx_expenses_date
    ON expenses (
        expense_date DESC
    );

CREATE INDEX idx_expenses_branch_date
    ON expenses (
        branch_id,
        expense_date DESC
    );

CREATE INDEX idx_expenses_category_date
    ON expenses (
        category,
        expense_date DESC
    );

CREATE INDEX idx_expenses_status_date
    ON expenses (
        status,
        expense_date DESC
    );