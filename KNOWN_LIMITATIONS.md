# Known Limitations — Operations Under Pressure

This document outlines intentional architectural boundaries, operational assumptions, and deferred enhancements in the current implementation of **Operations Under Pressure**.

---

## 1. Authentication & Session Management
- **Stateless Bearer Tokens in `sessionStorage`**: The frontend stores JWT tokens in browser `sessionStorage`. While this prevents cross-tab credential leakage and is automatically destroyed upon tab closure, tokens are technically accessible to client-side scripts if an XSS vulnerability exists. In a production enterprise deployment, `HttpOnly`, `SameSite=Strict`, `Secure` cookies with explicit CSRF token verification should be preferred.
- **Absence of Token Revocation / Blacklist**: JWT access tokens are cryptographically self-contained with a 24-hour expiration window. Once signed, tokens cannot be forcefully revoked before expiration unless an in-memory or Redis-backed distributed token blacklist is implemented.
- **Seeded Credentials vs. SSO / MFA**: The system uses deterministic database-seeded users for evaluation. Integration with enterprise identity providers (SAML 2.0 / OIDC / Okta) and multi-factor authentication (MFA) was intentionally deferred.

---

## 2. Notification Delivery & External Integration
- **Internal Audit Log Notification Handler**: The transactional outbox system processes domain events (such as `WORK_ITEM_CREATED`, `WORK_ITEM_ASSIGNED`, and `WORK_ITEM_STATUS_CHANGED`) using an idempotent in-process component (`OutboxEventHandler.java`). Physical delivery of SMTP emails, Slack webhooks, SMS alerts, or PagerDuty on-call escalations was intentionally omitted to eliminate external SaaS dependencies and API keys during local evaluation.
- **Outbox Table Retention & Purge**: Successfully processed (`COMPLETED`) outbox records remain in the `outbox_job` table for audit visibility. In high-volume production, an automated archival job (e.g., pruning records older than 30 days) is recommended to prevent table bloat.

---

## 3. Search & Pagination Strategy
- **Relational Filtering with SQL `LIKE`**: Server-side text search operates across `title` and `description` using PostgreSQL `LOWER(title) LIKE %pattern%`. While backed by standard indexes and team-scoped criteria, this does not provide advanced natural language stemming, typo tolerance, or TF-IDF relevance scoring that dedicated engines (such as Elasticsearch or PostgreSQL `tsvector` / GIN indexes) offer.
- **Offset-Based Pagination**: Work-item listing uses standard SQL `OFFSET` and `LIMIT` pagination via Spring Data's `Pageable`. For extremely deep pagination (e.g., browsing beyond page 5,000), keyset/cursor-based pagination (`WHERE id < :lastSeenId`) would be more performant.

---

## 4. Concurrent Workflow Edge Cases
- **Resolved to In-Progress Rework Transition**: The workflow engine permits transitioning an item from `RESOLVED` back to `IN_PROGRESS` only by team Members, Leads, or Admins. This accommodates operational rework if QA or verification fails. However, transitions out of `CLOSED` remain strictly forbidden across all roles.
- **Single-Worker Execution Assumption for Outbox**: The outbox worker implements atomic SQL conditional claiming (`claimJobAtomically`) to ensure multi-node safety. However, in extreme multi-master cluster topologies without synchronized system clocks, slight time skews could impact the exponential backoff schedule.

---

## 5. Deployment & Environment Prerequisites
- **Local Database Dependency**: The backend requires PostgreSQL (via Docker Compose or local installation). In environments where Docker Desktop daemon is stopped or cannot be launched, the application and tests can connect to a local PostgreSQL instance or WSL bridge.
