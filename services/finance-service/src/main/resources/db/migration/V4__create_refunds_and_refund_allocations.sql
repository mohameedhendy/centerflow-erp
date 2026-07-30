CREATE SEQUENCE refund_number_sequence
    START WITH 1
    INCREMENT BY 1;

ALTER TABLE payments
    ADD COLUMN refunded_amount NUMERIC(19, 2)
        NOT NULL DEFAULT 0;

ALTER TABLE payments
    ADD CONSTRAINT ck_payments_refunded_amount
        CHECK (
            refunded_amount >= 0
            AND refunded_amount <= amount
        );

ALTER TABLE payment_allocations
    ADD COLUMN refunded_amount NUMERIC(19, 2)
        NOT NULL DEFAULT 0;

ALTER TABLE payment_allocations
    ADD CONSTRAINT ck_payment_allocations_refunded
        CHECK (
            refunded_amount >= 0
            AND refunded_amount <= amount
        );

CREATE TABLE refunds
(
    id                 UUID                     NOT NULL,
    refund_number      VARCHAR(30)              NOT NULL,
    payment_id         UUID                     NOT NULL,
    amount             NUMERIC(19, 2)           NOT NULL,
    currency           VARCHAR(3)               NOT NULL,
    reason             VARCHAR(500)             NOT NULL,
    external_reference VARCHAR(100),
    status             VARCHAR(30)              NOT NULL,
    recorded_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_refunds
        PRIMARY KEY (id),

    CONSTRAINT uk_refunds_number
        UNIQUE (refund_number),

    CONSTRAINT uk_refunds_external_reference
        UNIQUE (external_reference),

    CONSTRAINT fk_refunds_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments (id),

    CONSTRAINT ck_refunds_amount
        CHECK (amount > 0),

    CONSTRAINT ck_refunds_currency
        CHECK (
            CHAR_LENGTH(currency) = 3
            AND UPPER(currency) = currency
        ),

    CONSTRAINT ck_refunds_reason
        CHECK (CHAR_LENGTH(TRIM(reason)) > 0),

    CONSTRAINT ck_refunds_status
        CHECK (status IN ('RECORDED'))
);

CREATE TABLE refund_allocations
(
    id                    UUID                     NOT NULL,
    refund_id             UUID                     NOT NULL,
    payment_allocation_id UUID                     NOT NULL,
    installment_id        UUID                     NOT NULL,
    allocation_order      INTEGER                  NOT NULL,
    amount                NUMERIC(19, 2)           NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_refund_allocations
        PRIMARY KEY (id),

    CONSTRAINT fk_refund_allocations_refund
        FOREIGN KEY (refund_id)
        REFERENCES refunds (id),

    CONSTRAINT fk_refund_allocations_payment_allocation
        FOREIGN KEY (payment_allocation_id)
        REFERENCES payment_allocations (id),

    CONSTRAINT fk_refund_allocations_installment
        FOREIGN KEY (installment_id)
        REFERENCES installments (id),

    CONSTRAINT uk_refund_allocation_order
        UNIQUE (refund_id, allocation_order),

    CONSTRAINT ck_refund_allocation_order
        CHECK (allocation_order >= 1),

    CONSTRAINT ck_refund_allocation_amount
        CHECK (amount > 0)
);

CREATE INDEX idx_refunds_payment_recorded
    ON refunds (payment_id, recorded_at);

CREATE INDEX idx_refund_allocations_refund
    ON refund_allocations (
        refund_id,
        allocation_order
    );