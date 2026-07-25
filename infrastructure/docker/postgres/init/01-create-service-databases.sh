#!/usr/bin/env bash

set -Eeuo pipefail

required_variables=(
    IDENTITY_DB_NAME
    IDENTITY_DB_USER
    IDENTITY_DB_PASSWORD
    ACADEMIC_DB_NAME
    ACADEMIC_DB_USER
    ACADEMIC_DB_PASSWORD
    ENROLLMENT_DB_NAME
    ENROLLMENT_DB_USER
    ENROLLMENT_DB_PASSWORD
    FINANCE_DB_NAME
    FINANCE_DB_USER
    FINANCE_DB_PASSWORD
    NOTIFICATION_DB_NAME
    NOTIFICATION_DB_USER
    NOTIFICATION_DB_PASSWORD
)

for variable_name in "${required_variables[@]}"; do
    if [[ -z "${!variable_name:-}" ]]; then
        echo "Required environment variable is missing: ${variable_name}"
        exit 1
    fi
done

create_database_and_user() {
    local database_name="$1"
    local database_user="$2"
    local database_password="$3"

    echo "Initializing database '${database_name}' for user '${database_user}'"

    psql \
        --username "$POSTGRES_USER" \
        --dbname "$POSTGRES_DB" \
        --set ON_ERROR_STOP=1 \
        --set database_name="$database_name" \
        --set database_user="$database_user" \
        --set database_password="$database_password" <<'EOSQL'
SELECT format(
    'CREATE ROLE %I LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD %L',
    :'database_user',
    :'database_password'
)
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_roles
    WHERE rolname = :'database_user'
)
\gexec

SELECT format(
    'CREATE DATABASE %I OWNER %I',
    :'database_name',
    :'database_user'
)
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_database
    WHERE datname = :'database_name'
)
\gexec

SELECT format(
    'REVOKE CONNECT ON DATABASE %I FROM PUBLIC',
    :'database_name'
)
\gexec

SELECT format(
    'GRANT CONNECT ON DATABASE %I TO %I',
    :'database_name',
    :'database_user'
)
\gexec

SELECT format(
    'ALTER DATABASE %I SET timezone TO %L',
    :'database_name',
    'UTC'
)
\gexec

SELECT format(
    'ALTER ROLE %I SET timezone TO %L',
    :'database_user',
    'UTC'
)
\gexec
EOSQL
}

create_database_and_user \
    "$IDENTITY_DB_NAME" \
    "$IDENTITY_DB_USER" \
    "$IDENTITY_DB_PASSWORD"

create_database_and_user \
    "$ACADEMIC_DB_NAME" \
    "$ACADEMIC_DB_USER" \
    "$ACADEMIC_DB_PASSWORD"

create_database_and_user \
    "$ENROLLMENT_DB_NAME" \
    "$ENROLLMENT_DB_USER" \
    "$ENROLLMENT_DB_PASSWORD"

create_database_and_user \
    "$FINANCE_DB_NAME" \
    "$FINANCE_DB_USER" \
    "$FINANCE_DB_PASSWORD"

create_database_and_user \
    "$NOTIFICATION_DB_NAME" \
    "$NOTIFICATION_DB_USER" \
    "$NOTIFICATION_DB_PASSWORD"

echo "CenterFlow service databases initialized successfully"