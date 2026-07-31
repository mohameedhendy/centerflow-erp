INSERT INTO pricing_plans
(
    id,
    code,
    name,
    description,
    total_amount,
    currency,
    installment_count,
    initial_payment_amount,
    status,
    created_at,
    updated_at,
    version
)
VALUES
(
    '00000000-0000-0000-0000-000000000101',
    'REPORT-PLAN',
    'Financial Report Plan',
    'Financial report test pricing plan',
    1000.00,
    'EGP',
    2,
    500.00,
    'ACTIVE',
    TIMESTAMP WITH TIME ZONE
        '2026-07-01 10:00:00+00:00',
    TIMESTAMP WITH TIME ZONE
        '2026-07-01 10:00:00+00:00',
    0
);

INSERT INTO enrollment_financial_accounts
(
    id,
    enrollment_id,
    student_id,
    pricing_plan_id,
    pricing_plan_code,
    total_amount,
    currency,
    installment_count,
    initial_payment_amount,
    status,
    created_at,
    updated_at,
    version,
    paid_amount
)
VALUES
(
    '00000000-0000-0000-0000-000000000201',
    '00000000-0000-0000-0000-000000000202',
    '00000000-0000-0000-0000-000000000203',
    '00000000-0000-0000-0000-000000000101',
    'REPORT-PLAN',
    1100.00,
    'EGP',
    2,
    500.00,
    'OPEN',
    TIMESTAMP WITH TIME ZONE
        '2026-07-01 10:00:00+00:00',
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 10:00:00+00:00',
    0,
    600.00
);

INSERT INTO installments
(
    id,
    financial_account_id,
    installment_number,
    due_date,
    amount,
    paid_amount,
    status,
    created_at,
    updated_at,
    version
)
VALUES
(
    '00000000-0000-0000-0000-000000000301',
    '00000000-0000-0000-0000-000000000201',
    1,
    DATE '2026-07-01',
    500.00,
    500.00,
    'PAID',
    TIMESTAMP WITH TIME ZONE
        '2026-07-01 10:00:00+00:00',
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 10:00:00+00:00',
    0
);

INSERT INTO installments
(
    id,
    financial_account_id,
    installment_number,
    due_date,
    amount,
    paid_amount,
    status,
    created_at,
    updated_at,
    version
)
VALUES
(
    '00000000-0000-0000-0000-000000000302',
    '00000000-0000-0000-0000-000000000201',
    2,
    DATE '2026-07-15',
    600.00,
    100.00,
    'OVERDUE',
    TIMESTAMP WITH TIME ZONE
        '2026-07-01 10:00:00+00:00',
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 10:00:00+00:00',
    0
);

INSERT INTO payments
(
    id,
    payment_number,
    financial_account_id,
    amount,
    currency,
    method,
    external_reference,
    status,
    recorded_at,
    version,
    refunded_amount
)
VALUES
(
    '00000000-0000-0000-0000-000000000401',
    'PAY-REPORT-001',
    '00000000-0000-0000-0000-000000000201',
    700.00,
    'EGP',
    'CARD',
    'PAY-REPORT-REF-001',
    'PARTIALLY_REFUNDED',
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 10:00:00+00:00',
    0,
    100.00
);

INSERT INTO refunds
(
    id,
    refund_number,
    payment_id,
    amount,
    currency,
    reason,
    external_reference,
    status,
    recorded_at,
    version
)
VALUES
(
    '00000000-0000-0000-0000-000000000501',
    'REF-REPORT-001',
    '00000000-0000-0000-0000-000000000401',
    100.00,
    'EGP',
    'Financial report test refund',
    'REF-REPORT-EXT-001',
    'RECORDED',
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 11:00:00+00:00',
    0
);

INSERT INTO financial_adjustments
(
    id,
    financial_account_id,
    type,
    amount,
    currency,
    reason,
    external_reference,
    created_at
)
VALUES
(
    '00000000-0000-0000-0000-000000000601',
    '00000000-0000-0000-0000-000000000201',
    'DISCOUNT',
    50.00,
    'EGP',
    'Financial report test discount',
    'ADJ-REPORT-DISCOUNT',
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 09:00:00+00:00'
);

INSERT INTO financial_adjustments
(
    id,
    financial_account_id,
    type,
    amount,
    currency,
    reason,
    external_reference,
    created_at
)
VALUES
(
    '00000000-0000-0000-0000-000000000602',
    '00000000-0000-0000-0000-000000000201',
    'CHARGE',
    150.00,
    'EGP',
    'Financial report test charge',
    'ADJ-REPORT-CHARGE',
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 09:30:00+00:00'
);

INSERT INTO expenses
(
    id,
    expense_number,
    branch_id,
    category,
    amount,
    currency,
    payment_method,
    payee,
    description,
    expense_date,
    external_reference,
    status,
    cancellation_reason,
    created_at,
    updated_at,
    cancelled_at,
    version
)
VALUES
(
    '00000000-0000-0000-0000-000000000701',
    'EXP-REPORT-001',
    NULL,
    'RENT',
    200.00,
    'EGP',
    'BANK_TRANSFER',
    'Report Property Owner',
    'Financial report recorded rent',
    DATE '2026-07-31',
    'EXP-REPORT-EXT-001',
    'RECORDED',
    NULL,
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 08:00:00+00:00',
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 08:00:00+00:00',
    NULL,
    0
);

INSERT INTO expenses
(
    id,
    expense_number,
    branch_id,
    category,
    amount,
    currency,
    payment_method,
    payee,
    description,
    expense_date,
    external_reference,
    status,
    cancellation_reason,
    created_at,
    updated_at,
    cancelled_at,
    version
)
VALUES
(
    '00000000-0000-0000-0000-000000000702',
    'EXP-REPORT-002',
    NULL,
    'OTHER',
    50.00,
    'EGP',
    'CASH',
    'Cancelled Payee',
    'Cancelled financial report expense',
    DATE '2026-07-31',
    'EXP-REPORT-EXT-002',
    'CANCELLED',
    'Expense entered incorrectly',
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 08:00:00+00:00',
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 09:00:00+00:00',
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 09:00:00+00:00',
    0
);

INSERT INTO instructor_earnings
(
    id,
    earning_number,
    instructor_id,
    session_id,
    batch_id,
    amount,
    currency,
    session_date,
    description,
    status,
    payment_method,
    payment_reference,
    cancellation_reason,
    accrued_at,
    paid_at,
    cancelled_at,
    created_at,
    updated_at,
    version
)
VALUES
(
    '00000000-0000-0000-0000-000000000801',
    'ERN-REPORT-001',
    '00000000-0000-0000-0000-000000000811',
    '00000000-0000-0000-0000-000000000812',
    '00000000-0000-0000-0000-000000000813',
    300.00,
    'EGP',
    DATE '2026-07-31',
    'Paid financial report earning',
    'PAID',
    'BANK_TRANSFER',
    'ERN-REPORT-PAYMENT-001',
    NULL,
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 07:00:00+00:00',
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 12:00:00+00:00',
    NULL,
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 07:00:00+00:00',
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 12:00:00+00:00',
    0
);

INSERT INTO instructor_earnings
(
    id,
    earning_number,
    instructor_id,
    session_id,
    batch_id,
    amount,
    currency,
    session_date,
    description,
    status,
    payment_method,
    payment_reference,
    cancellation_reason,
    accrued_at,
    paid_at,
    cancelled_at,
    created_at,
    updated_at,
    version
)
VALUES
(
    '00000000-0000-0000-0000-000000000802',
    'ERN-REPORT-002',
    '00000000-0000-0000-0000-000000000821',
    '00000000-0000-0000-0000-000000000822',
    '00000000-0000-0000-0000-000000000823',
    150.00,
    'EGP',
    DATE '2026-07-31',
    'Accrued financial report earning',
    'ACCRUED',
    NULL,
    NULL,
    NULL,
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 07:00:00+00:00',
    NULL,
    NULL,
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 07:00:00+00:00',
    TIMESTAMP WITH TIME ZONE
        '2026-07-31 07:00:00+00:00',
    0
);