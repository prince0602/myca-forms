--liquibase formatted sql

--changeset forms:010-form-response-pdf
-- S3 key of the PDF rendered from a submitted response. Written once, after commit, by FormResponsePdfService;
-- NULL means "not rendered yet" (legacy rows, or rendering still pending/failed) and is backfilled on first preview.
-- Both columns are nullable without defaults, so PostgreSQL adds them as a metadata-only change (no table rewrite).
ALTER TABLE form_responses
    ADD COLUMN IF NOT EXISTS pdf_key          VARCHAR(512),
    ADD COLUMN IF NOT EXISTS pdf_generated_at TIMESTAMPTZ;
--rollback ALTER TABLE form_responses DROP COLUMN IF EXISTS pdf_generated_at, DROP COLUMN IF EXISTS pdf_key;

--changeset forms:011-form-response-pdf-status
-- Persistent render state so failures back off instead of re-rendering on every preview, concurrent workers cannot
-- render the same response twice (claims are conditional UPDATEs), and a crashed PROCESSING job is recovered once its
-- lease (pdf_processing_started_at) expires. pdf_last_error holds a stage/exception code only, never patient data.
-- New columns are nullable or have a constant default, so PostgreSQL adds them without rewriting the table.
ALTER TABLE form_responses
    ADD COLUMN IF NOT EXISTS pdf_status                VARCHAR(20),
    ADD COLUMN IF NOT EXISTS pdf_attempts              INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS pdf_next_attempt_at       TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS pdf_processing_started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS pdf_failed_at             TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS pdf_last_error            VARCHAR(300);
ALTER TABLE form_responses
    ADD CONSTRAINT ck_form_responses_pdf_status
        CHECK (pdf_status IS NULL OR pdf_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'));
-- Rows rendered by changeset 010's code already have a key; everything else stays NULL (rendered lazily on preview).
UPDATE form_responses SET pdf_status = 'COMPLETED' WHERE pdf_key IS NOT NULL AND pdf_status IS NULL;
-- Only unfinished jobs are indexed, so the retry sweeper's lookup stays tiny however many responses exist.
CREATE INDEX IF NOT EXISTS ix_form_responses_pdf_due
    ON form_responses (pdf_status, pdf_next_attempt_at)
    WHERE pdf_status IN ('PENDING', 'PROCESSING', 'FAILED');
--rollback DROP INDEX IF EXISTS ix_form_responses_pdf_due;
--rollback ALTER TABLE form_responses DROP CONSTRAINT IF EXISTS ck_form_responses_pdf_status;
--rollback ALTER TABLE form_responses DROP COLUMN IF EXISTS pdf_last_error, DROP COLUMN IF EXISTS pdf_failed_at, DROP COLUMN IF EXISTS pdf_processing_started_at, DROP COLUMN IF EXISTS pdf_next_attempt_at, DROP COLUMN IF EXISTS pdf_attempts, DROP COLUMN IF EXISTS pdf_status;
