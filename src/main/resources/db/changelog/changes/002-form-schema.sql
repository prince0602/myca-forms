--liquibase formatted sql

--changeset forms:001-form-templates
CREATE TABLE IF NOT EXISTS form_templates (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(300)  NOT NULL,
    description      VARCHAR(2000),
    status           VARCHAR(20)   NOT NULL,
    current_version  INTEGER       NOT NULL,
    lock_version     BIGINT,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    created_at       TIMESTAMPTZ,
    updated_at       TIMESTAMPTZ,
    CONSTRAINT ck_form_templates_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
--rollback DROP TABLE form_templates;

--changeset forms:002-form-template-versions
CREATE TABLE IF NOT EXISTS form_template_versions (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id       UUID          NOT NULL,
    version_number    INTEGER       NOT NULL,
    label             VARCHAR(300)  NOT NULL,
    description       VARCHAR(2000),
    expiration_hours  INTEGER,
    form_fields       JSONB         NOT NULL,
    created_by        VARCHAR(255),
    created_at        TIMESTAMPTZ
);
--rollback DROP TABLE form_template_versions;

--changeset forms:003-sent-forms
CREATE TABLE IF NOT EXISTS sent_forms (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id       UUID          NOT NULL,
    template_version  INTEGER       NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    sent_at           TIMESTAMPTZ   NOT NULL,
    patient_id        VARCHAR(64)   NOT NULL,
    recipient_email   VARCHAR(320),
    expires_at        TIMESTAMPTZ,
    opened_at         TIMESTAMPTZ,
    submitted_at      TIMESTAMPTZ,
    revoked_at        TIMESTAMPTZ,
    created_by        VARCHAR(255),
    CONSTRAINT ck_sent_forms_status CHECK (status IN
        ('SENT', 'OPENED', 'IN_PROGRESS', 'SUBMITTED', 'EXPIRED', 'REVOKED'))
);
--rollback DROP TABLE sent_forms;

--changeset forms:004-form-links
CREATE TABLE IF NOT EXISTS form_links (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash        VARCHAR(255)  NOT NULL,
    sent_form_id      UUID          NOT NULL,
    expires_at        TIMESTAMPTZ,
    used_at           TIMESTAMPTZ,
    revoked_at        TIMESTAMPTZ,
    last_accessed_at  TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ux_form_links_token_hash UNIQUE (token_hash)
);
--rollback DROP TABLE form_links;

--changeset forms:005-form-responses
CREATE TABLE IF NOT EXISTS form_responses (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    sent_form_id      UUID          NOT NULL,
    template_id       UUID          NOT NULL,
    template_version  INTEGER       NOT NULL,
    answers           JSONB         NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    submitted_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    lock_version      BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ux_form_responses_sent_form UNIQUE (sent_form_id)
);
--rollback DROP TABLE form_responses;

--changeset forms:006-saved-components
CREATE TABLE IF NOT EXISTS saved_components (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(300)  NOT NULL,
    content     JSONB         NOT NULL,
    fields      JSONB         NOT NULL,
    created_by  VARCHAR(255),
    created_at  TIMESTAMPTZ   NOT NULL
);
--rollback DROP TABLE saved_components;

--changeset forms:007-form-indexes
CREATE INDEX IF NOT EXISTS ix_sent_forms_template_sent ON sent_forms (template_id, sent_at DESC);

CREATE INDEX IF NOT EXISTS ix_sent_forms_template_patient ON sent_forms (template_id, patient_id, sent_at DESC);

CREATE INDEX IF NOT EXISTS ix_form_templates_status_name ON form_templates (status, upper(name));

CREATE INDEX IF NOT EXISTS ix_form_templates_updated ON form_templates (updated_at DESC);
--rollback DROP INDEX ix_form_templates_updated;
--rollback DROP INDEX ix_form_templates_status_name;
--rollback DROP INDEX ix_sent_forms_template_patient;
--rollback DROP INDEX ix_sent_forms_template_sent;

--changeset forms:008-template-version-unique
--preconditions onFail:CONTINUE onFailMessage:form_template_versions has duplicate (template_id, version_number) rows; fix them, then restart
--precondition-sql-check expectedResult:0 SELECT count(*) FROM (SELECT 1 FROM form_template_versions GROUP BY template_id, version_number HAVING count(*) > 1) d
CREATE UNIQUE INDEX IF NOT EXISTS ux_form_template_versions_template_version ON form_template_versions (template_id, version_number);
--rollback DROP INDEX ux_form_template_versions_template_version;
