-- Schema initialization for Operations Under Pressure (newtonite-ops)

-- 1. App User table
CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 2. Team table
CREATE TABLE IF NOT EXISTS team (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 3. Team Membership table
CREATE TABLE IF NOT EXISTS team_membership (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES team(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_team_membership UNIQUE (user_id, team_id)
);

CREATE INDEX IF NOT EXISTS idx_team_membership_user ON team_membership(user_id);
CREATE INDEX IF NOT EXISTS idx_team_membership_team ON team_membership(team_id);

-- 4. Work Item table
CREATE TABLE IF NOT EXISTS work_item (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    team_id UUID NOT NULL REFERENCES team(id),
    assignee_id UUID REFERENCES app_user(id),
    created_by_id UUID NOT NULL REFERENCES app_user(id),
    due_date TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_work_item_team ON work_item(team_id);
CREATE INDEX IF NOT EXISTS idx_work_item_status ON work_item(status);
CREATE INDEX IF NOT EXISTS idx_work_item_priority ON work_item(priority);
CREATE INDEX IF NOT EXISTS idx_work_item_type ON work_item(type);
CREATE INDEX IF NOT EXISTS idx_work_item_assignee ON work_item(assignee_id);
CREATE INDEX IF NOT EXISTS idx_work_item_created_at ON work_item(created_at);

-- 5. Activity Event table
CREATE TABLE IF NOT EXISTS activity_event (
    id UUID PRIMARY KEY,
    work_item_id UUID NOT NULL REFERENCES work_item(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES app_user(id),
    actor_name VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_activity_event_work_item ON activity_event(work_item_id);
CREATE INDEX IF NOT EXISTS idx_activity_event_created_at ON activity_event(created_at);

-- 6. Comment table
CREATE TABLE IF NOT EXISTS comment (
    id UUID PRIMARY KEY,
    work_item_id UUID NOT NULL REFERENCES work_item(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES app_user(id),
    author_name VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_comment_work_item ON comment(work_item_id);
CREATE INDEX IF NOT EXISTS idx_comment_created_at ON comment(created_at);

-- 7. Idempotency Record table
CREATE TABLE IF NOT EXISTS idempotency_record (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    actor_id UUID NOT NULL REFERENCES app_user(id),
    operation VARCHAR(50) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_status INT NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_idempotency_record UNIQUE (idempotency_key, actor_id, operation)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_lookup ON idempotency_record(idempotency_key, actor_id, operation);

-- 8. Outbox Job table
CREATE TABLE IF NOT EXISTS outbox_job (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_job_poll ON outbox_job(status, next_attempt_at);
