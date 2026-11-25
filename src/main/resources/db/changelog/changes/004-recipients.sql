--liquibase formatted sql

--changeset workspace-design:013-patients-to-recipients
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT count(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'patients'
ALTER TABLE patients RENAME TO recipients;
ALTER INDEX IF EXISTS ix_patients_workspace RENAME TO ix_recipients_workspace;
ALTER TABLE recipients
    ADD COLUMN first_name VARCHAR(100),
    ADD COLUMN last_name  VARCHAR(100);
UPDATE recipients
   SET first_name = left(split_part(btrim(full_name), ' ', 1), 100),
       last_name  = left(btrim(substr(btrim(full_name), length(split_part(btrim(full_name), ' ', 1)) + 1)), 100);
ALTER TABLE recipients
    ALTER COLUMN first_name   SET NOT NULL,
    ALTER COLUMN last_name    SET NOT NULL,
    ALTER COLUMN address      DROP NOT NULL,
    ALTER COLUMN phone_number DROP NOT NULL,
    ALTER COLUMN dob          DROP NOT NULL,
    DROP COLUMN full_name;
--rollback ALTER TABLE recipients ADD COLUMN full_name VARCHAR(200);
--rollback UPDATE recipients SET full_name = left(btrim(first_name || ' ' || last_name), 200);
--rollback ALTER TABLE recipients ALTER COLUMN full_name SET NOT NULL, DROP COLUMN first_name, DROP COLUMN last_name;
--rollback ALTER INDEX IF EXISTS ix_recipients_workspace RENAME TO ix_patients_workspace;
--rollback ALTER TABLE recipients RENAME TO patients;

--changeset workspace-design:014-recipients-identity
--preconditions onFail:CONTINUE onFailMessage:recipients has duplicate (workspace, email, first_name, last_name) rows; merge them, then restart to create ux_recipients_identity
--precondition-sql-check expectedResult:0 SELECT count(*) FROM (SELECT 1 FROM recipients GROUP BY workspace_id, email, lower(first_name), lower(last_name) HAVING count(*) > 1) d
CREATE UNIQUE INDEX IF NOT EXISTS ux_recipients_identity ON recipients (workspace_id, email, lower(first_name), lower(last_name));
--rollback DROP INDEX ux_recipients_identity;

--changeset forms:009-sent-forms-recipient-id
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT count(*) FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'sent_forms' AND column_name = 'patient_id'
ALTER TABLE sent_forms RENAME COLUMN patient_id TO recipient_id;
ALTER INDEX IF EXISTS ix_sent_forms_template_patient RENAME TO ix_sent_forms_template_recipient;
--rollback ALTER INDEX IF EXISTS ix_sent_forms_template_recipient RENAME TO ix_sent_forms_template_patient;
--rollback ALTER TABLE sent_forms RENAME COLUMN recipient_id TO patient_id;
