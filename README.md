# Newtonite Ops — Operations Management System

## Overview

Newtonite Ops is a web-based operations management platform built using Java and Spring Boot. It helps teams manage incidents, assign tasks, track progress, and handle approval workflows through a centralized system.

The application focuses on secure team collaboration, reliable database operations, and consistent handling of concurrent requests.

## Key Features

- **Work Item Management:** Create, update, assign, claim, and resolve operational tasks.
- **Role-Based Access:** Permissions for Admins, Leads, Members, and Viewers.
- **Team Isolation:** Restricts access to resources based on team membership.
- **Concurrency Control:** Uses atomic database updates and optimistic locking to prevent conflicting changes.
- **Idempotent Requests:** Helps avoid duplicate records when requests are retried.
- **Activity Tracking:** Maintains a history of important work-item changes.
- **Dashboard:** Displays assigned tasks, urgent items, and pending approvals.
- **Background Processing:** Uses a transactional outbox for reliable event handling.

## Technology Stack

- Java 21
- Spring Boot 3
- Spring Security and JWT
- Spring Data JPA and Hibernate
- PostgreSQL and Flyway
- HTML, CSS, and JavaScript
- Maven and Docker Compose

## Architecture

The application follows a layered architecture:

1. **Frontend:** Provides the dashboard and work-item interface.
2. **Controllers:** Handle incoming REST API requests.
3. **Service Layer:** Implements business logic, authorization, and workflow rules.
4. **Database Layer:** Stores users, teams, work items, activity records, and background jobs.
5. **Outbox Worker:** Processes pending events and retries eligible failures.

## Getting Started

### Prerequisites

- JDK 21
- Docker with Docker Compose
- Git

### Installation

Clone the repository:

```bash
git clone https://github.com/prabhkalraa/newtonite-ops-23131180344.git
cd newtonite-ops-23131180344
```

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Build the application:

```bash
./mvnw clean package
```

Run the application:

```bash
./mvnw spring-boot:run
```

Open `http://localhost:8080` in your browser.

### Run Tests

```bash
./mvnw test
```

## API Highlights

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/login` | User authentication |
| GET | `/api/teams` | Retrieve accessible teams |
| GET | `/api/items` | List work items |
| POST | `/api/items` | Create a work item |
| POST | `/api/items/{id}/claim` | Claim an unassigned item |
| POST | `/api/items/{id}/assign` | Assign a work item |
| GET | `/api/dashboard/summary` | View dashboard metrics |

## Reliability and Security

The system uses JWT-based authentication, BCrypt password hashing, and resource-level authorization. Database transactions, optimistic locking, idempotency keys, and conditional updates help maintain data consistency. The transactional outbox supports reliable background event processing.

## Future Enhancements

- Expanded automated integration testing
- Improved monitoring and reporting
- Enhanced dashboard filtering
- Deployment automation

---

**Newtonite Ops** — Simplifying team coordination through secure and reliable operational workflows.
