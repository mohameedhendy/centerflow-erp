# CenterFlow ERP

CenterFlow ERP is a backend-only educational center management system built using Java and Spring Boot with a microservices architecture.

The system is designed to manage the academic, enrollment, financial, attendance, and notification workflows of educational centers and institutes.

## Project Status

The project is currently in the planning and foundation stage.

The initial architecture and service boundaries have been defined. Application services and infrastructure will be implemented incrementally using small and focused commits.

## Core Services

| Service                | Responsibility                                                                        |
| ---------------------- | ------------------------------------------------------------------------------------- |
| `api-gateway`          | Routes external requests and validates access tokens                                  |
| `identity-service`     | Authentication, users, roles, permissions, and token management                       |
| `academic-service`     | Branches, classrooms, courses, batches, instructors, schedules, and attendance        |
| `enrollment-service`   | Students, enrollments, capacity workflow, transfers, suspension, and cancellation     |
| `finance-service`      | Pricing, invoices, installments, payments, refunds, expenses, and instructor earnings |
| `notification-service` | Stores and manages notifications generated from domain events                         |

A dedicated `reporting-service` may be added after the MVP is complete.

## Planned Technology Stack

* Java 21
* Spring Boot
* Spring Security
* Spring Cloud Gateway
* PostgreSQL
* Spring Data JPA
* Hibernate
* Flyway
* Docker
* Docker Compose
* Kafka
* JUnit 5
* Mockito
* MockMvc
* Testcontainers
* Swagger / OpenAPI
* GitHub Actions
* Spring Boot Actuator

Advanced technologies such as Kafka and Resilience4j will only be introduced when a project requirement justifies them.

## Architecture Principles

* Each microservice owns its database.
* Services cannot access another service's database directly.
* No shared database is used.
* No shared JPA entities are used between services.
* Services communicate using IDs, DTOs, internal APIs, and domain events.
* Business rules are validated inside the service that owns the relevant data.
* Search, filtering, sorting, and pagination are executed in the database.
* Financial values use `BigDecimal`.
* Application entities are not returned directly from REST controllers.
* Technologies and patterns are introduced only when they solve a clear problem.

## Repository Structure

```text
centerflow-erp/
├── docs/
├── infrastructure/
│   └── docker/
├── services/
│   ├── api-gateway/
│   ├── identity-service/
│   ├── academic-service/
│   ├── enrollment-service/
│   ├── finance-service/
│   └── notification-service/
├── pom.xml
└── README.md
```

## Documentation

The initial architectural decisions, service boundaries, and data ownership rules are documented in:

```text
docs/architecture.md
```

## Development Approach

The project will be implemented incrementally.

Each step will include:

1. A clear technical or business objective.
2. The reason the feature is required.
3. Complete implementation files.
4. Tests for important business rules.
5. Local verification.
6. A small and meaningful Git commit.

## Current Phase

### Phase 1: Planning and Foundation

* [x] Define the MVP
* [x] Define service boundaries
* [x] Define data ownership
* [x] Define service communication
* [ ] Create the repository structure
* [ ] Create the initial services and local databases
