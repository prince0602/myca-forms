--liquibase formatted sql

--changeset workspace-design:012-baa-s3-url
--preconditions onFail:HALT onFailMessage:Set spring.liquibase.parameters.baa.s3-url to the public https URL of baa-2026.1.pdf
--precondition-sql-check expectedResult:0 SELECT CASE WHEN '${baa.s3-url}' LIKE 'https://%' THEN 0 ELSE 1 END
UPDATE baa_documents
   SET content_url = '${baa.s3-url}'
 WHERE version = '2026.1'
   AND content_hash = 'db4cfe5ab9c8a34a75ff05f9204bbf66b6f5292c61155e8c3f7319ba73296a4e';
--rollback empty
