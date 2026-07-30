CREATE SEQUENCE payment_number_sequence
    START WITH 1
    INCREMENT BY 1;

ALTER TABLE enrollment_financial_accounts
    ADD COLUMN paid_amount NUMERIC(19, 2)
        NOT NULL DEFAULT 0;

ALTER TABLE enrollment_financial_accounts
    ADD CONSTRAINT ck_financial_accounts_paid_amount
        CHECK (
            paid_amount >= 0
            AND paid_amount <= total_amount
        );

CREATE TABLE payments
(
    id                   UUID                     NOT NULL,
    payment_number       VARCHAR(30)              NOT NULL,
    financial_account_id UUID                     NOT NULL,
    amount               NUMERIC(19, 2)           NOT NULL,
    currency             VARCHAR(3)               NOT NULL,
    method               VARCHAR(30)              NOT NULL,
    external_reference   VARCHAR(100),
    status               VARCHAR(30)              NOT NULL,
    recorded_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    version              BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_payments
        PRIMARY KEY (id),

    CONSTRAINT uk_payments_number
        UNIQUE (payment_number),

    CONSTRAINT uk_payments_external_reference
        UNIQUE (external_reference),

    CONSTRAINT fk_payments_financial_account
        FOREIGN KEY (financial_account_id)
        REFERENCES enrollment_financial_accounts (id),

    CONSTRAINT ck_payments_amount
        CHECK (amount > 0),

    CONSTRAINT ck_payments_currency
        CHECK (
            CHAR_LENGTH(currency) = 3
            AND UPPER(currency) = currency
        ),

    CONSTRAINT ck_payments_method
        CHECK (
            method IN (
                'CASH',
                'CARD',
                'BANK_TRANSFER',
                'MOBILE_WALLET'
            )
        ),

    CONSTRAINT ck_payments_status
        CHECK (
            status IN (
                'RECORDED',
                'PARTIALLY_REFUNDED',
                'REFUNDED'
            )
        )
);

CREATE TABLE payment_allocations
(
    id               UUID                     NOT NULL,
    payment_id       UUID                     NOT NULL,
    installment_id   UUID                     NOT NULL,
    allocation_order INTEGER                  NOT NULL,
    amount           NUMERIC(19, 2)           NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_payment_allocations
        PRIMARY KEY (id),

    CONSTRAINT fk_payment_allocations_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments (id),

    CONSTRAINT fk_payment_allocations_installment
        FOREIGN KEY (installment_id)
        REFERENCES installments (id),

    CONSTRAINT uk_payment_allocation_installment
        UNIQUE (payment_id, installment_id),

    CONSTRAINT uk_payment_allocation_order
        UNIQUE (payment_id, allocation_order),

    CONSTRAINT ck_payment_allocation_order
        CHECK (allocation_order >= 1),

    CONSTRAINT ck_payment_allocation_amount
        CHECK (amount > 0)
);

CREATE INDEX idx_payments_account_recorded
    ON payments (
        financial_account_id,
        recorded_at
    );

CREATE INDEX idx_payment_allocations_payment
    ON payment_allocations (
        payment_id,
        allocation_order
    );