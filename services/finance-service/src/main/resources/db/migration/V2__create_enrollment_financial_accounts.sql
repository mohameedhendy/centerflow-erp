CREATE TABLE enrollment_financial_accounts
(
    id                     UUID                     NOT NULL,
    enrollment_id          UUID                     NOT NULL,
    student_id             UUID                     NOT NULL,
    pricing_plan_id        UUID                     NOT NULL,
    pricing_plan_code      VARCHAR(30)              NOT NULL,
    total_amount           NUMERIC(19, 2)           NOT NULL,
    currency               VARCHAR(3)               NOT NULL,
    installment_count      INTEGER                  NOT NULL,
    initial_payment_amount NUMERIC(19, 2)           NOT NULL,
    status                 VARCHAR(20)              NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    version                BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_enrollment_financial_accounts
        PRIMARY KEY (id),

    CONSTRAINT uk_financial_accounts_enrollment
        UNIQUE (enrollment_id),

    CONSTRAINT fk_financial_accounts_pricing_plan
        FOREIGN KEY (pricing_plan_id)
        REFERENCES pricing_plans (id),

    CONSTRAINT ck_financial_accounts_total
        CHECK (total_amount > 0),

    CONSTRAINT ck_financial_accounts_currency
        CHECK (
            CHAR_LENGTH(currency) = 3
            AND UPPER(currency) = currency
        ),

    CONSTRAINT ck_financial_accounts_installment_count
        CHECK (
            installment_count >= 1
            AND installment_count <= 60
        ),

    CONSTRAINT ck_financial_accounts_initial_payment
        CHECK (
            initial_payment_amount >= 0
            AND initial_payment_amount <= total_amount
        ),

    CONSTRAINT ck_financial_accounts_status
        CHECK (status IN ('OPEN', 'SETTLED', 'CANCELLED'))
);

CREATE TABLE installments
(
    id                 UUID                     NOT NULL,
    financial_account_id UUID                   NOT NULL,
    installment_number INTEGER                  NOT NULL,
    due_date           DATE                     NOT NULL,
    amount             NUMERIC(19, 2)           NOT NULL,
    paid_amount        NUMERIC(19, 2)           NOT NULL DEFAULT 0,
    status             VARCHAR(30)              NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_installments
        PRIMARY KEY (id),

    CONSTRAINT fk_installments_financial_account
        FOREIGN KEY (financial_account_id)
        REFERENCES enrollment_financial_accounts (id),

    CONSTRAINT uk_installments_account_number
        UNIQUE (
            financial_account_id,
            installment_number
        ),

    CONSTRAINT ck_installments_number
        CHECK (installment_number >= 1),

    CONSTRAINT ck_installments_amount
        CHECK (amount > 0),

    CONSTRAINT ck_installments_paid_amount
        CHECK (
            paid_amount >= 0
            AND paid_amount <= amount
        ),

    CONSTRAINT ck_installments_status
        CHECK (
            status IN (
                'PENDING',
                'PARTIALLY_PAID',
                'PAID',
                'OVERDUE',
                'CANCELLED'
            )
        )
);

CREATE INDEX idx_financial_accounts_student
    ON enrollment_financial_accounts (student_id);

CREATE INDEX idx_financial_accounts_status
    ON enrollment_financial_accounts (status);

CREATE INDEX idx_installments_account
    ON installments (
        financial_account_id,
        installment_number
    );

CREATE INDEX idx_installments_due_status
    ON installments (due_date, status);