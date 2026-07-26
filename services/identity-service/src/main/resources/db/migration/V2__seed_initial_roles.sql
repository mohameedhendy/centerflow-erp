INSERT INTO roles (
    id,
    name,
    description,
    created_at,
    updated_at
)
VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'ADMIN',
        'Full access to CenterFlow ERP',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'BRANCH_MANAGER',
        'Manages the operations of assigned branches',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'ACCOUNTANT',
        'Manages financial operations and reports',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000004',
        'INSTRUCTOR',
        'Accesses teaching schedules and academic operations',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000005',
        'RECEPTIONIST',
        'Manages students and enrollment operations',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000006',
        'STUDENT',
        'Accesses personal enrollment and academic information',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );