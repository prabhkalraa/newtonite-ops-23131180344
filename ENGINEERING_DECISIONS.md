# Engineering Decisions — Operations Under Pressure

This document outlines key architectural and engineering decisions made during the design and implementation of the **Operations Under Pressure** operational management platform.

---

## 1. Normalized Relational Schema & Append-Only Audit History

### Context & Problem
Operational workflows require complete transparency, regulatory compliance, and non-repudiation. When high-priority incidents or payment approvals occur, stakeholders need to know who made a change, what changed (old vs. new values), and when. If audit events are stored as mutable fields or updated in-place, history is lost or vulnerable to corruption.

### Options Considered
1. **Hibernate Envers / CDC (Change Data Capture)**: Transparently versions all entity tables with audit shadow tables.
2. **Document-style JSON Log in WorkItem**: Store an array of events directly in a JSONB column on the `work_item` row.
3. **Dedicated Append-Only `activity_event` Relational Table**: Normalized table referencing `work_item(id)` and `app_user(id)`, enforced via `Propagation.MANDATORY` in Spring transactions.

### Selected Approach
**Dedicated Append-Only `activity_event` Relational Table** (`ActivityEvent` entity).

### Why It Was Selected
- Enforces strict relational foreign keys and database-level indexes on `(work_item_id, created_at)`.
- No endpoints exist to edit or delete historical records.
- By binding event generation to Spring's transaction boundary (`@Transactional(propagation = Propagation.MANDATORY)`), business logic failure automatically rolls back the audit event, preventing orphan or phantom history.

### Trade-offs & Limitations
- Requires manual event invocation across service modification points rather than transparent ORM interception.
- High-volume workflows generate significant row counts, which eventually requires database partitioning or archival strategies in large-scale deployments.

---

## 2. Concurrency Control: Atomic Conditional Claims & Optimistic Locking

### Context & Problem
Multiple team members can view the same urgent unassigned incident simultaneously. If two engineers click "Claim" at the same instant, or if Engineer A updates priority while Engineer B is updating due dates, naive read-modify-write patterns cause race conditions and lost updates.

### Options Considered
1. **Pessimistic Row Locking (`SELECT ... FOR UPDATE`)**: Locks the row in PostgreSQL during read until the transaction finishes.
2. **Distributed Locks (e.g., Redis Redlock)**: External distributed coordination.
3. **Atomic Conditional SQL Update for Claims + JPA `@Version` for Stale Updates**:
   - Atomic conditional update: `UPDATE work_item SET assignee_id = :user, version = version + 1 WHERE id = :id AND assignee_id IS NULL`.
   - Optimistic locking: Client supplies the version it last read; server verifies version matches before saving.

### Selected Approach
**Atomic Conditional SQL Update for Claims + JPA `@Version` Optimistic Locking**.

### Why It Was Selected
- Eliminates database deadlocks associated with long-held pessimistic locks.
- The atomic claim query operates directly in the PostgreSQL storage engine in a single statement. If 0 rows are affected, it proves another transaction won the race, returning an immediate `409 Conflict`.
- Stale updates return HTTP 409 with an explicit instruction: *"This item has changed since you opened it. Reload the latest version before editing."*

### Trade-offs & Limitations
- If high contention occurs on general field updates, users must reload and re-apply changes.
- Requires the client application to manage and send `version` tokens across all mutating REST requests.

---

## 3. Resource-Level Team-Scoped Authorization

### Context & Problem
Enterprise operational teams (e.g., Infrastructure, Payment Services, Compliance, Support) work in separate domains. An engineer belonging to Team A must not view, claim, or edit Team B's incidents. Furthermore, roles vary per team: a user may be a Lead in Support but only a Viewer in Infrastructure.

### Options Considered
1. **Global Role Flags (e.g., Spring Security `hasRole('ADMIN')`)**: Assigns permissions globally across the application.
2. **Frontend-Only Role Checks**: Hiding buttons in HTML/JavaScript while leaving backend endpoints open.
3. **Centralized Backend `AuthorizationService` with Multi-Team Roles**: Every request derives the authenticated principal from the JWT security context, loads team memberships, and validates team ownership and minimum role (`VIEWER`, `MEMBER`, `LEAD`, `ADMIN`).

### Selected Approach
**Centralized Backend `AuthorizationService` (`AuthorizationService.java`)**.

### Why It Was Selected
- Never trusts client-supplied user IDs, roles, or team IDs.
- Prevents cross-team data leaks: querying work items or comments automatically filters against `user.getTeamIds()`.
- Strict role boundaries: Only Leads or Admins within that specific team can approve items in `AWAITING_APPROVAL` or reassign tickets.
- Centralization avoids scattering permission logic across multiple controllers.

### Trade-offs & Limitations
- Every resource access requires loading or validating team membership. Optimized using JPA entity graphs and indexed composite keys `(user_id, team_id)`.

---

## 4. Idempotent Create Requests via `Idempotency-Key`

### Context & Problem
Network dropouts, browser retries, or double-clicks on "Create Work Item" can result in duplicate incident records being created, confusing on-call responders and polluting queues.

### Options Considered
1. **Client-Side Debouncing Only**: Disabling the submit button in JavaScript.
2. **In-Memory Cache (e.g., Guava/Caffeine)**: Storing recent request keys in memory.
3. **Persistent `idempotency_record` Table with Request Body SHA-256 Hashes**: Database-backed record storing `(idempotency_key, actor_id, operation, request_hash, response_status, response_body)`.

### Selected Approach
**Persistent `idempotency_record` Table with SHA-256 Payload Hashes**.

### Why It Was Selected
- Client-side debouncing is insufficient against network replays or multi-tab requests.
- In-memory caching loses idempotency upon application restarts or horizontally scaled replicas.
- Storing the SHA-256 hash ensures that identical retries replay the original `201 Created` response without duplicate insertion. Reusing the same key with altered payload parameters triggers a strict `409 Conflict`.

### Trade-offs & Limitations
- Adds an additional database lookup and insert per creation operation.
- Requires payload serialization to JSON to calculate deterministic SHA-256 hashes.

---

## 5. Reliable Transactional Outbox & Exponential Backoff Retry Worker

### Context & Problem
When a work item is created or assigned, external downstream systems (such as notification services, audit streams, or messaging webhooks) must be notified. Publishing directly to external services within the database transaction risks dual-write anomalies (e.g., DB commits but notification fails, or notification succeeds but DB rolls back).

### Options Considered
1. **Direct Synchronous Calls in Service**: Call notifications directly inside `@Transactional`.
2. **Spring Application Events (`@TransactionalEventListener`)**: Internal in-memory events.
3. **Transactional Outbox Table (`outbox_job`) with Scheduled Background Worker**: Writing the domain event to `outbox_job` in the exact same database transaction as the business entity.

### Selected Approach
**Transactional Outbox Table with Scheduled Worker (`OutboxWorker.java`)**.

### Why It Was Selected
- Guarantees at-least-once delivery without dual-write inconsistency.
- Worker uses atomic conditional row claiming (`claimJobAtomically`) to ensure multiple worker threads or cluster nodes never execute the same event concurrently.
- Supports bounded exponential backoff retry (`2^attempts * 2` seconds).
- Reaches terminal `FAILED` state with recorded error log after maximum attempts (default: 5) to prevent infinite poison pill loops.
- `OutboxEventHandler` is idempotent to eliminate duplicate side effects on retries.

### Trade-offs & Limitations
- Near real-time delivery with a short polling interval (3 seconds) rather than instant pub/sub message bus (e.g., Kafka/RabbitMQ).
- Outbox table requires periodic purge of old `COMPLETED` records in high-throughput environments.

---

## 6. Server-Side Filtering, Pagination, and Database Indexes

### Context & Problem
Large operational platforms track thousands of work items. Fetching all items to filter in memory causes memory exhaustion, slow responses, and unbounded network latency.

### Options Considered
1. **Client-Side Data Table Filtering**: Loading full collections into JavaScript and filtering with array methods.
2. **Full-Text Search Engine (Elasticsearch / Solr)**: External search cluster.
3. **Spring Data JPA `JpaSpecificationExecutor` with PostgreSQL B-Tree Indexes**: Dynamic criteria query executed directly in PostgreSQL with `Pageable` parameters.

### Selected Approach
**Spring Data JPA Specifications with Database Indexes**.

### Why It Was Selected
- Keeps infrastructure simple, reliable, and runnable without third-party search clusters.
- Filters by team, status, priority, type, assignee, date range, and case-insensitive keyword search across `title` and `description`.
- Backed by Flyway indexes on `(team_id)`, `(status)`, `(priority)`, `(assignee_id)`, and `(created_at)`.
- Enforces team authorization at the database query level so unauthorized data is never retrieved from PostgreSQL into application memory.

### Trade-offs & Limitations
- Free-text `LIKE %search%` queries can be slower on millions of records compared to PostgreSQL `tsvector` full-text search or dedicated Elasticsearch clusters.

---

## 7. Plain HTML/CSS/Vanilla JavaScript Frontend Architecture

### Context & Problem
The challenge mandates strict technical constraints prohibiting external node-based build systems, React, Vue, Angular, Bootstrap, and Tailwind CSS. The application must be runnable directly from Spring Boot's static resources on `http://localhost:8080`.

### Options Considered
1. **Server-Side Rendered Templates (Thymeleaf / JSP)**: Traditional multi-page rendering.
2. **Vanilla JavaScript Single-Page Application (SPA)**: HTML5 semantic structure, modern Vanilla CSS variables, and Fetch API.

### Selected Approach
**Vanilla JavaScript Single-Page Application (SPA) served via Spring Boot Static Resources**.

### Why It Was Selected
- Provides fluid enterprise UX with dynamic modal overlays, real-time KPI card updates, filter transitions, and tabbed activity logs without full page reloads.
- No Node.js, Webpack, or npm dependencies required—reviewer simply runs Maven.
- Safe DOM updates using `textContent` and `createElement` avoid cross-site scripting (XSS).
- Lightweight footprint (< 30 KB total frontend assets) ensuring sub-100ms load times.

### Trade-offs & Limitations
- Requires manual DOM state management rather than reactive two-way data binding.
- Manual URL hash / history management if complex deep-linking is required.
