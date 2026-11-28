--liquibase formatted sql

--changeset plans:001-plan-limits
-- Plan defaults live in the database so they are changed through the audited usage-admin script (LM1, LM7).
CREATE TABLE IF NOT EXISTS plan_limits (
    plan           VARCHAR(20)  PRIMARY KEY,
    monthly_limit  INTEGER      NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_plan_limits_plan          CHECK (plan IN ('FREE', 'PLUS')),
    CONSTRAINT ck_plan_limits_monthly_limit CHECK (monthly_limit >= 0)
);
INSERT INTO plan_limits (plan, monthly_limit) VALUES ('FREE', 20), ('PLUS', 400)
ON CONFLICT (plan) DO NOTHING;
--rollback DROP TABLE plan_limits;

--changeset plans:002-workspace-billing-cycle
-- Each workspace refreshes on its own billing date (P6). Existing workspaces are anchored to their creation time.
ALTER TABLE workspaces
    ADD COLUMN IF NOT EXISTS billing_interval  VARCHAR(10),
    ADD COLUMN IF NOT EXISTS billing_anchor_at TIMESTAMPTZ;
UPDATE workspaces SET billing_anchor_at = created_at WHERE billing_anchor_at IS NULL;
ALTER TABLE workspaces
    ALTER COLUMN billing_anchor_at SET NOT NULL,
    ADD CONSTRAINT ck_workspaces_billing_interval CHECK (billing_interval IS NULL OR billing_interval IN ('MONTHLY', 'ANNUAL')),
    ADD CONSTRAINT ck_workspaces_custom_monthly_limit CHECK (custom_monthly_limit IS NULL OR custom_monthly_limit >= 0);
--rollback ALTER TABLE workspaces DROP CONSTRAINT ck_workspaces_custom_monthly_limit, DROP CONSTRAINT ck_workspaces_billing_interval, DROP COLUMN billing_anchor_at, DROP COLUMN billing_interval;

--changeset plans:003-workspace-payments
-- One row per verified card payment; the unique reference makes upgrades idempotent and stops payment reuse.
CREATE TABLE IF NOT EXISTS workspace_payments (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id       UUID         NOT NULL REFERENCES workspaces (id),
    payment_reference  VARCHAR(100) NOT NULL,
    plan               VARCHAR(20)  NOT NULL,
    billing_interval   VARCHAR(10)  NOT NULL,
    amount_cents       BIGINT       NOT NULL,
    currency           VARCHAR(3)   NOT NULL,
    paid_by            UUID         NOT NULL REFERENCES users (id),
    paid_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_workspace_payments_reference UNIQUE (payment_reference),
    CONSTRAINT ck_workspace_payments_plan      CHECK (plan IN ('FREE', 'PLUS')),
    CONSTRAINT ck_workspace_payments_interval  CHECK (billing_interval IN ('MONTHLY', 'ANNUAL')),
    CONSTRAINT ck_workspace_payments_amount    CHECK (amount_cents > 0)
);
CREATE INDEX IF NOT EXISTS ix_workspace_payments_workspace ON workspace_payments (workspace_id, paid_at DESC);
--rollback DROP TABLE workspace_payments;

--changeset plans:004-workspace-bonuses-order-index
-- Bonus forms are consumed oldest first (LM4).
CREATE INDEX IF NOT EXISTS ix_workspace_bonuses_workspace_granted ON workspace_bonuses (workspace_id, granted_at, id);
--rollback DROP INDEX IF EXISTS ix_workspace_bonuses_workspace_granted;
