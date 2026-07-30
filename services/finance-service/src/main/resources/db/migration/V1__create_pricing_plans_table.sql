CREATE TABLE pricing_plans
(
    id                     UUID                     NOT NULL,
    code                   VARCHAR(30)              NOT NULL,
    name                   VARCHAR(150)             NOT NULL,
    description            VARCHAR(500),
    total_amount           NUMERIC(19, 2)           NOT NULL,
    currency               VARCHAR(3)               NOT NULL,
    installment_count      INTEGER                  NOT NULL,
    initial_payment_amount NUMERIC(19, 2)           NOT NULL,
    status                 VARCHAR(20)              NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    version                BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_pricing_plans
        PRIMARY KEY (id),

    CONSTRAINT uk_pricing_plans_code
        UNIQUE (code),

    CONSTRAINT ck_pricing_plans_code_not_blank
        CHECK (CHAR_LENGTH(TRIM(code)) > 0),

    CONSTRAINT ck_pricing_plans_name_not_blank
        CHECK (CHAR_LENGTH(TRIM(name)) > 0),

    CONSTRAINT ck_pricing_plans_total_amount
        CHECK (total_amount > 0),

    CONSTRAINT ck_pricing_plans_currency
        CHECK (
            CHAR_LENGTH(currency) = 3
            AND UPPER(currency) = currency
        ),

    CONSTRAINT ck_pricing_plans_installment_count
        CHECK (
            installment_count >= 1
            AND installment_count <= 60
        ),

    CONSTRAINT ck_pricing_plans_initial_payment
        CHECK (
            initial_payment_amount >= 0
            AND initial_payment_amount <= total_amount
        ),

    CONSTRAINT ck_pricing_plans_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_pricing_plans_status
    ON pricing_plans (status);

CREATE INDEX idx_pricing_plans_name
    ON pricing_plans (name);