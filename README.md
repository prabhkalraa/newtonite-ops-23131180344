# Newtonite Ops — Operations Under Pressure

A production-oriented operations management platform designed to help engineering, payment, risk, and customer support teams manage incidents, coordinate tasks, and handle approvals securely.

The system focuses on reliable workflows, team-based authorization, concurrency control, and consistent database operations.

## Key Features

- **Work Item Management:** Create, assign, claim, update, and resolve operational items.
- **Role-Based Authorization:** Control access using user roles and team memberships.
- **Concurrency Control:** Atomic claiming and optimistic locking help prevent conflicting updates.
- **Idempotency:** Prevent duplicate work items when equivalent requests are retried.
- **Activity History:** Maintain an append-only record of important changes.
- **Dashboard:** Access operational summaries and team-specific work items.
- **Transactional Outbox:** Process background events with retry support.
- **Secure Authentication:** JWT-based authentication and BCrypt password hashing.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Backend development |
| Spring Boot 3 | Application framework |
| Spring Security and JWT | Authentication and access control |
| Spring Data JPA / Hibernate | Data persistence |
| PostgreSQL | Relational database |
| Flyway | Database migrations |
| HTML, CSS, JavaScript | Frontend interface |
| Maven | Build and dependency management |
| Docker Compose | Database environment |

## System Architecture

The following diagram illustrates how the user interface, security layer, work coordination services, database, activity tracking, and background processing interact.

```mermaid
flowchart TB
    U(("Team User"))

    subgraph UI["User Interface"]
        B["Operations Browser<br/>app.js"]
    end

    subgraph SEC["Identity and Teams"]
        AUTH["Authentication<br/>AuthService"]
        JWT["JWT Validation<br/>Security Filter"]
        PRINCIPAL["User Principal<br/>UserPrincipal"]
        AZ["Team Authorization<br/>AuthorizationService"]
        MEMBERS[("Team Membership")]
        USERS[("User Records")]
    end

    subgraph WORK["Work Coordination"]
        API["Work Item API<br/>WorkItemController"]
        OPS["Work Item Operations<br/>WorkItemService"]
        COMMENTS["Comments API"]
        RULES["Workflow Rules<br/>WorkflowService"]
        ITEMS[("Work Items")]
        COMMENTDB[("Comment Records")]
    end

    subgraph REPORT["Activity and Reporting"]
        ACT["Activity Event Service"]
        HISTORY[("Activity Records")]
        DASH["Dashboard API"]
        SUMMARY["Dashboard Summary"]
    end

    subgraph REL["Reliable Processing"]
        IDEM["Idempotency Service"]
        IDEMDB[("Idempotency Records")]
        OUTBOX["Outbox Service"]
        JOBS[("Outbox Jobs")]
        WORKER["Scheduled Outbox Worker"]
        HANDLER["Event Handler"]
    end

    U --> B
    U --> AUTH
    B -->|"Login request"| AUTH
    AUTH -->|"JWT token"| B
    B -->|"Bearer token"| JWT
    JWT --> PRINCIPAL
    PRINCIPAL --> API

    AUTH --> USERS
    AUTH --> AZ
    AZ --> MEMBERS
    API --> AZ
    API --> OPS
    API --> COMMENTS
    COMMENTS --> COMMENTDB

    OPS --> RULES
    OPS --> ITEMS
    OPS --> IDEM
    IDEM --> IDEMDB

    OPS -->|"Record changes"| ACT
    ACT --> HISTORY
    OPS -->|"Publish event"| OUTBOX
    OUTBOX --> JOBS
    WORKER -->|"Claim pending jobs"| JOBS
    WORKER --> OUTBOX
    OUTBOX --> HANDLER

    API --> DASH
    DASH --> ITEMS
    DASH --> HISTORY
    DASH --> SUMMARY

    classDef user fill:#dbeafe,stroke:#4f83ff,color:#1e3a8a
    classDef work fill:#fff0cc,stroke:#e89b24,color:#854d0e
    classDef report fill:#dcfce7,stroke:#36a663,color:#166534
    classDef reliable fill:#ffe4e6,stroke:#f43f5e,color:#9f1239
    classDef security fill:#dbeafe,stroke:#4f83ff,color:#1e3a8a

    class U,B user
    class AUTH,JWT,PRINCIPAL,AZ,MEMBERS,USERS security
    class API,OPS,COMMENTS,RULES,ITEMS,COMMENTDB work
    class ACT,HISTORY,DASH,SUMMARY report
    class IDEM,IDEMDB,OUTBOX,JOBS,WORKER,HANDLER reliable
```

## API Highlights

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Authenticate a user |
| `GET` | `/api/teams` | List accessible teams |
| `GET` | `/api/items` | Retrieve work items |
| `POST` | `/api/items` | Create a work item |
| `POST` | `/api/items/{id}/claim` | Claim an item |
| `POST` | `/api/items/{id}/assign` | Assign an item |
| `GET` | `/api/dashboard/summary` | Retrieve dashboard metrics |

## Getting Started

### Prerequisites

- JDK 21
- Git
- Docker with Docker Compose

### 1. Clone the Repository

```bash
git clone https://github.com/prabhkalraa/newtonite-ops-23131180344.git
cd newtonite-ops-23131180344
```

### 2. Start PostgreSQL

```bash
docker compose up -d postgres
```

### 3. Build the Project

```bash
chmod +x mvnw
./mvnw clean package
```

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

Open `http://localhost:8080` in your browser.

### 5. Run Tests

```bash
./mvnw test
```

## Reliability and Security

Newtonite Ops combines JWT authentication, team-level permission checks, database transactions, optimistic locking, and idempotency handling. Its transactional outbox separates event persistence from background processing, while activity records provide an audit trail of operational changes.

Review `ENGINEERING_DECISIONS.md` and `KNOWN_LIMITATIONS.md` for additional architectural details and documented limitations.

## Future Improvements

- Expanded integration and concurrency testing
- Enhanced dashboard reporting
- Improved monitoring and observability
- Deployment automation

---

**Newtonite Ops** — Reliable workflows, secure collaboration, and consistent operations.
