--liquibase formatted sql

--changeset workspace-design:001-baa-documents
CREATE TABLE IF NOT EXISTS baa_documents (
    version         VARCHAR(20)  PRIMARY KEY,
    content_hash    VARCHAR(64)  NOT NULL,
    content_url     VARCHAR(500) NOT NULL,
    effective_from  TIMESTAMPTZ  NOT NULL,
    is_current      BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_baa_documents_current ON baa_documents (is_current) WHERE is_current;
--rollback DROP TABLE baa_documents;

--changeset workspace-design:002-users
CREATE TABLE IF NOT EXISTS users (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_user_id       UUID         NOT NULL,
    account_id             VARCHAR(12)  NOT NULL,
    email                  VARCHAR(320) NOT NULL,
    first_name             VARCHAR(100),
    last_name              VARCHAR(100),
    phone_e164             VARCHAR(16),
    profession             VARCHAR(40),
    profession_other       VARCHAR(100),
    team_size              VARCHAR(20),
    onboarding_status      VARCHAR(20)  NOT NULL,
    personal_workspace_id  UUID,
    default_workspace_id   UUID,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ux_users_keycloak_user_id      UNIQUE (keycloak_user_id),
    CONSTRAINT ux_users_account_id            UNIQUE (account_id),
    CONSTRAINT ux_users_email                 UNIQUE (email),
    CONSTRAINT ux_users_personal_workspace_id UNIQUE (personal_workspace_id),
    CONSTRAINT ck_users_onboarding_status CHECK (onboarding_status IN ('PROFILE_REQUIRED', 'COMPLETE')),
    CONSTRAINT ck_users_team_size CHECK (team_size IS NULL OR team_size IN
        ('JUST_ME', 'SIZE_2_10', 'SIZE_11_50', 'SIZE_51_200', 'SIZE_201_PLUS'))
);
--rollback DROP TABLE users;

--changeset workspace-design:003-workspaces
CREATE TABLE IF NOT EXISTS workspaces (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id             VARCHAR(6)   NOT NULL,
    name                     VARCHAR(200) NOT NULL,
    account_type             VARCHAR(10)  NOT NULL,
    plan                     VARCHAR(20)  NOT NULL,
    custom_monthly_limit     INTEGER,
    forms_used               INTEGER      NOT NULL DEFAULT 0,
    usage_refresh_at         TIMESTAMPTZ,
    baa_version              VARCHAR(20)  REFERENCES baa_documents (version),
    baa_accepted_by          UUID         REFERENCES users (id),
    baa_accepted_at          TIMESTAMPTZ,
    baa_accepted_ip          VARCHAR(45),
    baa_accepted_user_agent  VARCHAR(512),
    payment_reference        VARCHAR(100),
    created_by               UUID         NOT NULL REFERENCES users (id),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                  BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ux_workspaces_workspace_id UNIQUE (workspace_id),
    CONSTRAINT ck_workspaces_account_type CHECK (account_type IN ('PERSONAL', 'BUSINESS')),
    CONSTRAINT ck_workspaces_plan CHECK (plan IN ('FREE', 'PLUS')),
    CONSTRAINT ck_workspaces_forms_used CHECK (forms_used >= 0)
);
CREATE INDEX IF NOT EXISTS ix_workspaces_created_by ON workspaces (created_by);
--rollback DROP TABLE workspaces;

--changeset workspace-design:003b-users-workspace-fks
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_constraint WHERE conname IN ('fk_users_personal_workspace', 'fk_users_default_workspace')
ALTER TABLE users
    ADD CONSTRAINT fk_users_personal_workspace FOREIGN KEY (personal_workspace_id) REFERENCES workspaces (id),
    ADD CONSTRAINT fk_users_default_workspace  FOREIGN KEY (default_workspace_id)  REFERENCES workspaces (id) ON DELETE SET NULL;
--rollback ALTER TABLE users DROP CONSTRAINT fk_users_default_workspace, DROP CONSTRAINT fk_users_personal_workspace;

--changeset workspace-design:004-memberships
CREATE TABLE IF NOT EXISTS memberships (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users (id),
    workspace_id    UUID         NOT NULL REFERENCES workspaces (id),
    role            VARCHAR(10)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    invited_by      UUID         REFERENCES users (id),
    joined_at       TIMESTAMPTZ  NOT NULL,
    access_ends_at  TIMESTAMPTZ,
    ended_at        TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT ux_memberships_user_workspace UNIQUE (user_id, workspace_id),
    CONSTRAINT ck_memberships_role   CHECK (role IN ('OWNER', 'MEMBER')),
    CONSTRAINT ck_memberships_status CHECK (status IN ('ACTIVE', 'REMOVED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS ix_memberships_workspace_status ON memberships (workspace_id, status, joined_at);

CREATE INDEX IF NOT EXISTS ix_memberships_user_status ON memberships (user_id, status);

CREATE INDEX IF NOT EXISTS ix_memberships_access_ends ON memberships (access_ends_at)
    WHERE status = 'ACTIVE' AND access_ends_at IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_memberships_one_owner ON memberships (workspace_id) WHERE role = 'OWNER';
--rollback DROP TABLE memberships;

--changeset workspace-design:005-invitations
CREATE TABLE IF NOT EXISTS invitations (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID         NOT NULL REFERENCES workspaces (id),
    email        VARCHAR(320) NOT NULL,
    token_hash   VARCHAR(64)  NOT NULL,
    role         VARCHAR(10)  NOT NULL,
    invited_by   UUID         NOT NULL REFERENCES users (id),
    status       VARCHAR(20)  NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    accepted_by  UUID         REFERENCES users (id),
    accepted_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ux_invitations_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_invitations_role   CHECK (role IN ('OWNER', 'MEMBER')),
    CONSTRAINT ck_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_invitations_pending_email ON invitations (workspace_id, email) WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS ix_invitations_workspace_status ON invitations (workspace_id, status);
--rollback DROP TABLE invitations;

--changeset workspace-design:006-workspace-bonuses
CREATE TABLE IF NOT EXISTS workspace_bonuses (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID         NOT NULL REFERENCES workspaces (id),
    amount       INTEGER      NOT NULL,
    forms_used   INTEGER      NOT NULL DEFAULT 0,
    reason       VARCHAR(255) NOT NULL,
    granted_by   UUID         NOT NULL REFERENCES users (id),
    granted_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_workspace_bonuses_amount CHECK (amount > 0),
    CONSTRAINT ck_workspace_bonuses_used   CHECK (forms_used >= 0 AND forms_used <= amount)
);
CREATE INDEX IF NOT EXISTS ix_workspace_bonuses_workspace ON workspace_bonuses (workspace_id);
--rollback DROP TABLE workspace_bonuses;

--changeset workspace-design:007-audit-events
CREATE TABLE IF NOT EXISTS audit_events (
    id             BIGINT       GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    occurred_at    TIMESTAMPTZ  NOT NULL,
    action         VARCHAR(60)  NOT NULL,
    actor_user_id  UUID,
    workspace_id   UUID,
    target         VARCHAR(100),
    ip             VARCHAR(45),
    user_agent     VARCHAR(512),
    details        JSONB
);
CREATE INDEX IF NOT EXISTS ix_audit_events_workspace ON audit_events (workspace_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_audit_events_actor     ON audit_events (actor_user_id, occurred_at DESC);
--rollback DROP TABLE audit_events;

--changeset workspace-design:008-seed-baa context:dev
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM baa_documents
INSERT INTO baa_documents (version, content_hash, content_url, effective_from, is_current)
VALUES ('2026.1',
        'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
        'https://example.com/legal/baa-2026.1.pdf',
        now(),
        TRUE);
--rollback DELETE FROM baa_documents WHERE version = '2026.1';

--changeset workspace-design:009-baa-static-url
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM baa_documents WHERE version = '2026.1' AND content_hash = 'db4cfe5ab9c8a34a75ff05f9204bbf66b6f5292c61155e8c3f7319ba73296a4e'
INSERT INTO baa_documents (version, content_hash, content_url, effective_from, is_current)
VALUES ('2026.1',
        'db4cfe5ab9c8a34a75ff05f9204bbf66b6f5292c61155e8c3f7319ba73296a4e',
        '${baa.content-url}',
        now(),
        TRUE)
ON CONFLICT (version) DO UPDATE
   SET content_url  = EXCLUDED.content_url,
       content_hash = EXCLUDED.content_hash;
--rollback empty

--changeset workspace-design:010-patients
CREATE TABLE IF NOT EXISTS patients (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  UUID         NOT NULL REFERENCES workspaces (id),
    full_name     VARCHAR(200) NOT NULL,
    address       VARCHAR(500) NOT NULL,
    email         VARCHAR(320) NOT NULL,
    phone_number  VARCHAR(25)  NOT NULL,
    dob           DATE         NOT NULL,
    created_by    UUID         NOT NULL REFERENCES users (id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version       BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS ix_patients_workspace ON patients (workspace_id, created_at);
--rollback DROP TABLE patients;

--changeset workspace-design:011-invitations-resend-index
CREATE INDEX IF NOT EXISTS ix_invitations_workspace_email_created ON invitations (workspace_id, email, created_at);
--rollback DROP INDEX ix_invitations_workspace_email_created;
