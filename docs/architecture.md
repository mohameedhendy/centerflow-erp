# CenterFlow ERP Architecture

## 1. System Overview

CenterFlow ERP is a backend-only system for managing educational centers and institutes.

The implemented MVP covers:

- Authentication, JWT issuance, refresh sessions, logout, and role-based authorization.
- Secure first-administrator bootstrap and administrator role assignment.
- Branches, classrooms, courses, batches, instructors, schedules, and attendance.
- Students and enrollment lifecycle management.
- Pricing plans, financial accounts, installments, payments, allocations, refunds, adjustments, and expenses.
- Instructor earnings.
- Academic and financial reports.
- Overdue installment processing.
- Persistent in-app notifications.
- Cross-service enrollment activation after the required initial payment.

The system is a focused educational-center ERP. It is not intended to be a complete accounting platform or a general-purpose enterprise ERP.

## 2. Architecture Style

The system uses a microservices architecture in a monorepo.

Each data-owning service:

- Has a clear business boundary.
- Owns its domain model and business rules.
- Owns its database and Flyway migrations.
- Exposes APIs through DTOs.
- Does not access another service's database.
- Communicates with other services through identifiers and internal REST APIs.

The project does not use:

- A shared application database.
- Shared JPA entities.
- A service for every entity.
- Eureka or another service registry.
- Spring Cloud Config Server.
- Kubernetes.
- Distributed transactions.
- A Saga framework.
- Kafka or another external message broker.

## 3. Deployment and Network Boundary

The default Docker Compose configuration publishes only the API Gateway on host port `8080`.

PostgreSQL and the five internal services are available only on the Docker network:

```text
postgres:5432
identity-service:8081
academic-service:8082
enrollment-service:8083
finance-service:8084
notification-service:8085
```

`compose.dev.yaml` is an explicit local-development override that publishes PostgreSQL and internal service ports for debugging and runtime verification.

The API Gateway also denies external requests targeting internal API paths. Network isolation and gateway authorization therefore provide separate protection layers.

## 4. Services

### 4.1 API Gateway

Responsibilities:

- Route external requests to the owning service.
- Validate JWT access tokens.
- Convert the token `roles` claim into Spring Security authorities.
- Apply role-based authorization rules.
- Permit only explicitly public authentication and actuator routes.
- Deny external access to internal service endpoints.

The gateway has no business database and does not contain domain logic.

### 4.2 Identity Service

Owns:

- Users.
- Password hashes.
- Roles.
- User-role assignments.
- Access-token issuance.
- Refresh-token sessions.
- Token revocation and logout state.
- Password-reset tokens.
- First-administrator bootstrap configuration.

Seeded roles:

```text
ADMIN
BRANCH_MANAGER
ACCOUNTANT
INSTRUCTOR
RECEPTIONIST
STUDENT
```

Administrator-only operations include replacing a user's role assignments.

The Identity Service prevents an administrator from removing their own `ADMIN` role. Administrator bootstrap is disabled by default, refuses to promote an existing account by email, and becomes idempotent after an administrator already exists.

### 4.3 Academic Service

Owns:

- Branches.
- Classrooms.
- Courses.
- Batches.
- Instructors.
- Schedules and class sessions.
- Attendance records.
- Batch capacity and seat rules.
- Academic reporting queries.

The Academic Service is the source of truth for batch capacity and seat availability.

### 4.4 Enrollment Service

Owns:

- Students.
- Enrollments.
- Enrollment lifecycle and current status.
- Enrollment filtering and pagination.
- Enrollment activation.

Implemented enrollment statuses include:

```text
PENDING_PAYMENT
ACTIVE
SUSPENDED
COMPLETED
CANCELLED
```

An enrollment starts in `PENDING_PAYMENT` and becomes `ACTIVE` only after Finance Service confirms that the configured initial payment requirement has been satisfied.

### 4.5 Finance Service

Owns:

- Pricing plans.
- Enrollment financial accounts.
- Installments.
- Payments.
- Payment allocations.
- Refunds.
- Financial adjustments.
- Expenses.
- Instructor earnings.
- Financial reports.
- Overdue installment processing.
- Persistent enrollment-activation tasks.

Implemented installment statuses include:

```text
PENDING
PARTIALLY_PAID
PAID
OVERDUE
CANCELLED
```

Supported payment methods include:

```text
CASH
CARD
BANK_TRANSFER
MOBILE_WALLET
```

All monetary values use `BigDecimal`.

### 4.6 Notification Service

Owns:

- Persistent in-app notifications.
- Notification status.
- Reference type and reference ID metadata.
- Event-source metadata.
- Filtering and pagination.
- Processed-event identifiers used for idempotency.

The current MVP stores in-app notifications. Email, SMS, and WhatsApp delivery are deferred.

## 5. Data Ownership

| Data | Owning Service |
| --- | --- |
| Users, credentials, roles, refresh sessions, and password-reset tokens | Identity Service |
| Branches, classrooms, courses, batches, instructors, schedules, and attendance | Academic Service |
| Students and enrollments | Enrollment Service |
| Pricing plans, financial accounts, installments, payments, refunds, expenses, and earnings | Finance Service |
| Notifications and processed notification events | Notification Service |

Cross-service references use identifiers rather than cross-database foreign keys.

Small immutable snapshots may be stored when a historical business record must remain stable after source data changes. A snapshot is not treated as a second owner of the source domain entity.

## 6. Communication Model

### 6.1 External Communication

External clients call the API Gateway.

The Gateway routes:

```text
/api/v1/auth/**           -> Identity Service
/api/v1/academic/**       -> Academic Service
/api/v1/enrollments/**    -> Enrollment Service
/api/v1/finance/**        -> Finance Service
/api/v1/notifications/**  -> Notification Service
```

### 6.2 Internal REST Communication

Synchronous internal REST calls are used when the current workflow requires an immediate response.

Examples include:

- Enrollment-related validation against Academic Service.
- Finance Service requesting enrollment activation.
- Finance Service requesting notification persistence.

Internal endpoints are blocked from external routing and are not host-published in the secure Docker configuration.

### 6.3 Process-Local Application Events

Spring application events are used inside a service process to separate transaction completion from follow-up work.

They are not a cross-service message broker.

For example, Finance Service can complete a payment transaction, publish a process-local after-commit event, and then process a persistent activation task in an independent transaction before calling Enrollment Service.

An external broker may be introduced later only when durable asynchronous cross-service delivery becomes an implemented requirement.

## 7. Enrollment and Payment Workflow

The implemented workflow is:

1. A pricing plan is created in Finance Service.
2. Enrollment Service creates an enrollment with `PENDING_PAYMENT` status.
3. Finance Service creates a financial account for the enrollment.
4. Finance Service generates installments from the selected pricing plan.
5. Finance Service records a payment.
6. The payment is allocated across installments.
7. Finance Service checks the configured initial-payment requirement.
8. When satisfied, Finance Service creates an enrollment-activation task.
9. After the payment transaction commits, Finance Service processes the task in an independent transaction.
10. Finance Service calls Enrollment Service through an internal API.
11. Enrollment Service changes the enrollment from `PENDING_PAYMENT` to `ACTIVE`.
12. Finance Service requests a payment notification.
13. Notification Service stores a `PAYMENT_RECORDED` notification.

The persistent activation task records processing status and attempts so a follow-up failure does not corrupt the original payment transaction.

## 8. Overdue Installment Workflow

1. Finance Service selects eligible unpaid installments.
2. The installment status changes to `OVERDUE`.
3. A notification request is sent to Notification Service.
4. Notification Service stores the overdue notification.
5. Repeated processing remains idempotent.

Scheduled production jobs are disabled in the automated test profile.

## 9. Database Strategy

The local environment uses one PostgreSQL container with separate databases:

```text
identity_db
academic_db
enrollment_db
finance_db
notification_db
```

Each service receives:

- A separate database.
- A separate database role.
- Separate credentials.
- Separate Flyway migrations.
- Permission to connect only to its own application database.

The shared PostgreSQL container is an infrastructure convenience, not a shared application schema.

## 10. Security Model

Security is enforced through multiple layers:

- JWT validation at the API Gateway.
- Role-based route authorization.
- Additional protection on Identity administrator endpoints.
- Administrator-only role assignment.
- Prevention of self-removal of the final role required by the current administrator session.
- Internal path denial at the Gateway.
- Docker network isolation for PostgreSQL and internal services.
- First-administrator bootstrap disabled by default.
- Bootstrap password removed from the recreated container after initialization.

JWT role claims are fixed for the lifetime of an issued access token. A role change therefore requires a new login or token refresh before the new authorities appear in a token.

## 11. Observability

Every service exposes:

```text
/actuator/health
/actuator/health/liveness
/actuator/health/readiness
/actuator/prometheus
```

Metrics include an `application` tag identifying the source service.

The runtime verification scripts wait for health endpoints before creating test fixtures.

## 12. Verification Strategy

The repository includes:

- Unit tests for domain and business rules.
- Repository tests.
- Controller integration tests.
- Security and authorization tests.
- Database-backed integration tests.
- Scheduler tests.
- Academic report runtime verification.
- Financial report runtime verification.
- Overdue installment runtime verification.
- Enrollment-payment end-to-end verification.
- RBAC runtime verification.
- Docker service-isolation verification.

Runtime fixtures are removed after verification.

## 13. Development Rules

- Use Java 21.
- Use UUID identifiers.
- Use UTC for timestamps.
- Use `BigDecimal` for money.
- Use DTOs at API boundaries.
- Do not expose JPA entities directly.
- Do not use Lombok `@Data` on JPA entities.
- Use only necessary annotations.
- Validate at API and business layers.
- Use centralized exception handling.
- Execute search, filtering, sorting, and pagination in database queries.
- Add indexes for implemented query requirements.
- Protect critical business rules with tests.
- Use database locking where concurrent writes can violate a rule.
- Prefer small and focused Git commits.
- Add infrastructure only when it solves an implemented requirement.

## 14. Deferred Features

The current MVP does not include:

- Exams and results.
- Certificates.
- Guardians.
- Online payment gateways.
- Full double-entry accounting.
- Payroll and taxes.
- Inventory.
- Email, WhatsApp, or SMS delivery.
- Multi-tenancy.
- Kubernetes.
- Eureka.
- Config Server.
- A dedicated reporting service.
- An external message broker.
