CREATE TABLE tenants (
    id              UUID PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    slug            VARCHAR(120) NOT NULL UNIQUE,
    logo_url        VARCHAR(500),
    plan            VARCHAR(32)  NOT NULL DEFAULT 'FREE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE TABLE memberships (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenants (id),
    user_id         UUID         NOT NULL REFERENCES users (id),
    role            VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, user_id)
);

CREATE TABLE invites (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenants (id),
    email           VARCHAR(255) NOT NULL,
    role            VARCHAR(32)  NOT NULL,
    token           VARCHAR(64)  NOT NULL UNIQUE,
    invited_by      UUID         REFERENCES users (id),
    expires_at      TIMESTAMPTZ  NOT NULL,
    accepted_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE companies (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenants (id),
    name            VARCHAR(200) NOT NULL,
    domain          VARCHAR(255),
    industry        VARCHAR(120),
    phone           VARCHAR(64),
    website         VARCHAR(255),
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_companies_tenant ON companies (tenant_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_companies_tenant_name ON companies (tenant_id, lower(name)) WHERE deleted_at IS NULL;

CREATE TABLE contacts (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenants (id),
    company_id      UUID         REFERENCES companies (id),
    first_name      VARCHAR(120) NOT NULL,
    last_name       VARCHAR(120) NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(64),
    title           VARCHAR(160),
    tags            VARCHAR(500),
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_contacts_tenant ON contacts (tenant_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_contacts_tenant_email ON contacts (tenant_id, lower(email)) WHERE email IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE deals (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID           NOT NULL REFERENCES tenants (id),
    title               VARCHAR(255)   NOT NULL,
    stage               VARCHAR(32)    NOT NULL,
    amount              NUMERIC(14, 2),
    currency            VARCHAR(8)     NOT NULL DEFAULT 'USD',
    company_id          UUID           REFERENCES companies (id),
    contact_id          UUID           REFERENCES contacts (id),
    owner_user_id       UUID           REFERENCES users (id),
    expected_close_date DATE,
    notes               TEXT,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ
);
CREATE INDEX idx_deals_tenant ON deals (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_deals_tenant_stage ON deals (tenant_id, stage) WHERE deleted_at IS NULL;

CREATE TABLE activities (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenants (id),
    type            VARCHAR(32)  NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    body            TEXT,
    due_at          TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    contact_id      UUID         REFERENCES contacts (id),
    company_id      UUID         REFERENCES companies (id),
    deal_id         UUID         REFERENCES deals (id),
    owner_user_id   UUID         REFERENCES users (id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_activities_tenant ON activities (tenant_id) WHERE deleted_at IS NULL;

CREATE TABLE mailboxes (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID         NOT NULL REFERENCES tenants (id),
    user_id                 UUID         NOT NULL REFERENCES users (id),
    provider                VARCHAR(32)  NOT NULL,
    email_address           VARCHAR(255) NOT NULL,
    display_name            VARCHAR(200),
    encrypted_credentials   TEXT,
    connected_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_sync_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMPTZ
);
CREATE INDEX idx_mailboxes_tenant ON mailboxes (tenant_id) WHERE deleted_at IS NULL;

CREATE TABLE mail_messages (
    id                   UUID PRIMARY KEY,
    tenant_id            UUID         NOT NULL REFERENCES tenants (id),
    mailbox_id           UUID         NOT NULL REFERENCES mailboxes (id),
    folder               VARCHAR(32)  NOT NULL,
    provider_message_id  VARCHAR(255),
    subject              VARCHAR(500),
    from_address         VARCHAR(255),
    to_addresses         TEXT,
    cc_addresses         TEXT,
    body_text            TEXT,
    body_html            TEXT,
    sent_at              TIMESTAMPTZ,
    contact_id           UUID         REFERENCES contacts (id),
    deal_id              UUID         REFERENCES deals (id),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_mail_tenant_folder ON mail_messages (tenant_id, mailbox_id, folder);

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenants (id),
    user_id         UUID,
    action          VARCHAR(80)  NOT NULL,
    entity_type     VARCHAR(80),
    entity_id       UUID,
    details         TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_tenant ON audit_logs (tenant_id, created_at DESC);

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users (id),
    tenant_id       UUID         REFERENCES tenants (id),
    token_hash      VARCHAR(128) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
