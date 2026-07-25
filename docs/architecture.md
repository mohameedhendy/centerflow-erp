# CenterFlow ERP Architecture

## 1. System Overview

CenterFlow ERP is a backend-only system for managing educational centers and institutes.

The system covers:

* Authentication and authorization
* Branch and classroom management
* Courses, levels, batches, and schedules
* Student registration and enrollment workflows
* Tuition plans, invoices, installments, and payments
* Expenses and instructor earnings
* Attendance
* Internal notifications
* Academic and financial reports

The first version is intended to be a focused educational center ERP and not a complete accounting or general-purpose enterprise ERP.

## 2. Architecture Style

The system uses a microservices architecture with a monorepo source-code structure.

Each service:

* Has one clear business responsibility
* Owns its data
* Owns its database migrations
* Exposes its own APIs
* Cannot access another service's database
* Communicates with other services using IDs, DTOs, internal APIs, and events

The project does not use:

* A shared database
* Shared JPA entities
* A service for every entity
* Eureka
* Config Server
* Kubernetes
* Distributed transactions
* A Saga framework during the initial implementation

## 3. Services

### API Gateway

Responsibilities:

* Route external requests
* Validate JWT access tokens
* Apply CORS configuration
* Add request correlation identifiers
* Prevent external access to internal service endpoints

The gateway does not contain business logic and does not own a database.

### Identity Service

Owns:

* Users
* Roles
* Permissions
* User-role assignments
* Role-permission assignments
* Refresh-token sessions
* Password-reset tokens
* User access scopes

Initial roles:

```text
ADMIN
BRANCH_MANAGER
ACCOUNTANT
INSTRUCTOR
RECEPTIONIST
STUDENT
```

### Academic Service

Owns:

* Branches
* Classrooms
* Courses
* Course levels
* Instructors
* Batches
* Class sessions
* Attendance records
* Batch seat reservations

The Academic Service is the source of truth for batch capacity and seat availability.

### Enrollment Service

Owns:

* Students
* Enrollments
* Enrollment status history
* Enrollment transfers
* Enrollment pricing snapshots

Enrollment statuses:

```text
PENDING_PAYMENT
ACTIVE
SUSPENDED
COMPLETED
CANCELLED
```

The current enrollment remains active after a valid batch transfer. Transfer history is stored separately instead of using `TRANSFERRED` as a current enrollment state.

### Finance Service

Owns:

* Tuition plans
* Invoices
* Invoice items
* Installments
* Payments
* Payment allocations
* Receipts
* Invoice discounts
* Refunds
* Expense categories
* Expenses
* Instructor compensation rules
* Instructor earnings
* Instructor payouts

Installment statuses:

```text
PENDING
PARTIALLY_PAID
PAID
OVERDUE
CANCELLED
```

Payment methods:

```text
CASH
CARD
BANK_TRANSFER
MOBILE_WALLET
```

All monetary values are stored using `BigDecimal`.

### Notification Service

Owns:

* Notifications
* Notification read status
* Processed event identifiers

The first version stores in-app notifications only. Email delivery may be added later.

## 4. Data Ownership

| Data                                                  | Owning Service       |
| ----------------------------------------------------- | -------------------- |
| Users, credentials, roles, and permissions            | Identity Service     |
| Branches, classrooms, courses, batches, and schedules | Academic Service     |
| Students and enrollments                              | Enrollment Service   |
| Pricing, invoices, installments, and payments         | Finance Service      |
| Notifications                                         | Notification Service |
| Attendance                                            | Academic Service     |
| Instructor academic profiles                          | Academic Service     |
| Instructor financial earnings                         | Finance Service      |

Cross-service references use IDs rather than cross-database foreign keys.

Historical documents may contain small data snapshots, such as:

* Student name on an invoice
* Course name on an invoice
* Agreed tuition amount on an enrollment

These snapshots are historical records and are not duplicated domain entities.

## 5. Synchronous Communication

Synchronous communication is used when the current request requires an immediate result.

Examples:

### Enrollment Service to Academic Service

* Validate a batch
* Reserve a seat
* Release a seat
* Reserve a seat during a transfer

Seat reservation must be atomic. The Enrollment Service must not retrieve the capacity and calculate availability locally.

### Enrollment Service to Finance Service

* Request the applicable tuition plan
* Receive a pricing quote before creating the enrollment

The accepted price is stored as an enrollment snapshot so future price changes do not modify existing enrollments.

### Academic Service to Enrollment Service

* Retrieve active enrollment IDs for a batch when recording attendance

Filtering is executed in the Enrollment Service database.

## 6. Event-Based Communication

Kafka will be introduced when an implemented workflow requires asynchronous communication.

Planned events include:

```text
EnrollmentCreated
EnrollmentCancelled
EnrollmentSuspended
EnrollmentTransferred
PaymentRecorded
EnrollmentPaymentSatisfied
InstallmentOverdue
SessionRescheduled
AttendanceAbsent
SessionCompleted
RefundCompleted
```

A payment does not automatically activate an enrollment.

The Finance Service publishes `EnrollmentPaymentSatisfied` only after the required initial payment amount has been reached.

## 7. Main Enrollment Workflow

1. Enrollment Service requests a pricing quote from Finance Service.
2. Enrollment Service asks Academic Service to reserve a batch seat.
3. Enrollment Service creates the enrollment with `PENDING_PAYMENT`.
4. An `EnrollmentCreated` event is published.
5. Finance Service creates the invoice and installments.
6. Notification Service creates an enrollment notification.

If enrollment creation fails after reserving the seat, the Enrollment Service requests the release of that reservation.

## 8. Payment Activation Workflow

1. Finance Service records the payment.
2. The payment is allocated to one or more installments.
3. Installment and invoice totals are updated.
4. A receipt is created.
5. Finance Service publishes `PaymentRecorded`.
6. If the required initial amount has been reached, Finance Service publishes `EnrollmentPaymentSatisfied`.
7. Enrollment Service changes the enrollment from `PENDING_PAYMENT` to `ACTIVE`.
8. Notification Service creates a payment confirmation.

## 9. Database Strategy

The following logical databases will be used:

```text
identity_db
academic_db
enrollment_db
finance_db
notification_db
```

During local development, the databases may run inside one PostgreSQL instance.

Each service receives:

* A separate database
* A separate database user
* Separate credentials
* Separate Flyway migrations

No service receives credentials for another service's database.

## 10. Development Rules

* Use Java 21.
* Use UUID identifiers.
* Use UTC for timestamps.
* Use `BigDecimal` for money.
* Use DTOs at API boundaries.
* Do not expose JPA entities directly.
* Do not use Lombok `@Data` on JPA entities.
* Use only necessary annotations.
* Apply validation at API and business layers.
* Use global exception handling.
* Execute search and filtering in database queries.
* Use pagination for collection endpoints.
* Add indexes based on real query requirements.
* Protect critical business rules with tests.
* Use database locking where concurrent writes can break a rule.
* Prefer small and focused Git commits.
* Add technologies only when they solve an implemented requirement.

## 11. Deferred Features

The initial MVP does not include:

* Exams and results
* Certificates
* Guardians
* Online payment gateways
* Full double-entry accounting
* Payroll and taxes
* Inventory
* WhatsApp or SMS
* Multi-tenancy
* Kubernetes
* Eureka
* Config Server
* A dedicated reporting service
