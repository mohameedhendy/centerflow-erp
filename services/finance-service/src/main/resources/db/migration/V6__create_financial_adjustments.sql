ALTER TABLE installments
    DROP CONSTRAINT ck_installments_amount;

ALTER TABLE installments
    ADD CONSTRAINT ck_installments_amount
        CHECK (amount >= 0);

CREATE TABLE financial_adjustments
(
    id                   UUID                     NOT NULL,
    financial_account_id UUID                     NOT NULL,
    type                 VARCHAR(20)              NOT NULL,
    amount               NUMERIC(19, 2)           NOT NULL,
    currency             VARCHAR(3)               NOT NULL,
    reason               VARCHAR(500)             NOT NULL,
    external_reference   VARCHAR(100),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_financial_adjustments
        PRIMARY KEY (id),

    CONSTRAINT fk_adjustments_financial_account
        FOREIGN KEY (financial_account_id)
        REFERENCES enrollment_financial_accounts (id),

    CONSTRAINT uk_adjustments_external_reference
        UNIQUE (external_reference),

    CONSTRAINT ck_adjustments_type
        CHECK (type IN ('DISCOUNT', 'CHARGE')),

    CONSTRAINT ck_adjustments_amount
        CHECK (amount > 0),

    CONSTRAINT ck_adjustments_currency
        CHECK (
            CHAR_LENGTH(currency) = 3
            AND UPPER(currency) = currency
        ),

    CONSTRAINT ck_adjustments_reason
        CHECK (
            CHAR_LENGTH(TRIM(reason)) >= 3
            AND CHAR_LENGTH(reason) <= 500
        ),

    CONSTRAINT ck_adjustments_external_reference
        CHECK (
            external_reference IS NULL
            OR CHAR_LENGTH(TRIM(external_reference)) > 0
        )
);

CREATE INDEX idx_adjustments_account_created
    ON financial_adjustments (
        financial_account_id,
        created_at DESC
    );

CREATE INDEX idx_adjustments_account_type
    ON financial_adjustments (
        financial_account_id,
        type
    );