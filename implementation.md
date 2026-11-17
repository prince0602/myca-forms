# Shareable Form Links: Implementation Plan

> **Status legend:** ✅ Done · 🟡 Partial · ⬜ Not started
> **Last reviewed against code:** 2026-11-17 (`main` @ `4a4ff98` + untracked `FormLinkRequest` / `FormLinkResponse`)

## Progress at a glance

| Phase | Status | Notes |
|---|---|---|
| 0. Prerequisites | ⬜ | Email template, `ApiException` handler and CORS all still missing. |
| 1. Data model | ✅ | `SentForm`, `FormLink`, `FormResponse` entities and repositories are in place. |
| 2. Link generation | 🟡 | `send-form-link` creates `SentForm` + token, pins the current version and applies expiry rules. Dedicated endpoint, `validate()` fix and "sent to" count remain. |
| 3. Public form API | ⬜ | |
| 4. Answer validation | ⬜ | |
| 5. Staff management | ⬜ | |
| 6. Uploads and tests | ⬜ | No `src/test` directory exists yet. |

---

## Summary of the flow

1. **Create the link (staff).** `POST /api/v1/form-templates/{id}/links` creates a `SentForm` for the patient and pins it to the template's current version. It generates a random token, stores only its SHA-256 hash in `form_links`, and returns `…/forms/fill?token=…`. If an email address is provided, the link is also emailed.
2. **Open the form (patient).** The frontend calls `GET /api/public/forms` with the token. This endpoint is public (no login). It returns the pinned form fields and any saved draft.
3. **Fill in and autosave.** `PUT /api/public/forms/draft` saves partial answers, so the patient can leave and come back using the same link.
4. **Submit.** `POST /api/public/forms/submit` validates the answers against the pinned fields and saves them as `SUBMITTED` in `form_responses`. The link is then closed.
5. **Review (staff).** `GET /api/v1/sent-forms/{id}/response` returns the fields with the patient's answers. Staff can also list, revoke and resend links.

Answers are stored as JSON keyed by field `id`: `{ "<fieldId>": value }`. Because template versions never change, those ids stay valid.

---

## 1. Current state of the codebase

### 1.1 Building blocks already in place

| Component | Status | Notes |
|---|---|---|
| `FormLinkService` | ✅ | 32-byte `SecureRandom` token, Base64-URL encoded; only the SHA-256 hash is stored. `create(sentFormId, expiresAt)`, `validate`, `markUsed`. (`validate` needs rework — see B7.) |
| `FormTemplateVersion` | ✅ | Immutable versions with `fields` as jsonb, plus `expirationHours`. Works for pinning. |
| `ImageUrlResolver` | ✅ | Turns body-diagram image names into presigned URLs. |
| `EmailService` / `FormEmailService` | ✅ | Thymeleaf-rendered email. Provider switch `app.email.provider`: `smtp` (default, via `SmtpEmailSender`) or `ses` (SDK). |
| `SentForm` entity | ✅ | UUID template id + pinned `templateVersion`, `@Enumerated(STRING)` status, `patientId`, `recipientEmail`, `expiresAt`, `openedAt`, `submittedAt`, `revokedAt`, `createdBy`. |
| `SentFormStatus` enum | ✅ | `SENT, OPENED, IN_PROGRESS, SUBMITTED, EXPIRED, REVOKED`. |
| `FormLink` entity | ✅ | `tokenHash`, `sentFormId` (UUID), `expiresAt`, `usedAt`, `revokedAt`, `lastAccessedAt`, `createdAt`. |
| `FormResponse` entity | ✅ | `sentFormId` (unique), `templateId`, `templateVersion`, `answers` (jsonb), `status` (String: DRAFT/SUBMITTED), timestamps, `@Version lockVersion`. |
| Repositories | ✅ | `FormLinkRepository.findByTokenHash`, `FormResponseRepository.findBySentFormId`, `SentFormRepository.countByTemplateIds`. |
| `POST /api/v1/emails/send-form-link` | ✅ | Creates `SentForm` (status SENT) + `FormLink`, emails `{environment-url}/forms/fill?token=…`. Requires JWT; `createdBy` = JWT subject. |
| `FormLinkRequest` / `FormLinkResponse` DTOs | 🟡 | Untracked. `FormLinkResponse` is ready. `FormLinkRequest` only has `expiresInHours` — needs `patientId` and optional `email`. |

### 1.2 Blockers / gaps

| # | Problem | Where | Status |
|---|---|---|---|
| B1 | The `form-request` Thymeleaf template is missing (`src/main/resources/templates/` does not exist), so sending a link fails at render time. | `EmailService`, `src/main/resources/templates/` | ⬜ |
| B2 | `FormLink` used `Long formId/patientId` and wasn't tied to a template version. | `entity/FormLink.java` | ✅ Now references `sentFormId` (UUID); version is pinned on `SentForm`. |
| B3 | `SentForm` was never created; status was a plain string; enum lacked statuses. | `entity/SentForm.java`, `enums/SentFormStatus.java` | ✅ Enum extended, `@Enumerated(STRING)`, new columns added, created by `FormEmailService.sendFormLink`. |
| B4 | `ApiException` has no handler, so "invalid link" / "expired link" come back as HTTP 500. Also `FormLinkService` throws `badRequest` for every case instead of 404/410. | `exception/FormsExceptionHandler.java`, `ApiException`, `FormLinkService` | ⬜ |
| B5 | Every endpoint requires a Keycloak login, so a patient cannot open the form. | `config/SecurityConfig.java` | ⬜ |
| B6 | No response table, entity or API. | — | 🟡 Entity + repository done; API not started. |
| B7 | `validate()` rejects a link once `usedAt` is set (breaks drafts/return visits), doesn't check `revokedAt`, and NPEs when `expiresAt` is null. `usedAt` should only be set on submit. | `FormLinkService` | ⬜ |
| B8 | Expiry was hard-coded to 72h, ignored the template's `expirationHours`, and `SendFormLinkDto` capped it at 168h. | `FormEmailService`, `SendFormLinkDto` | ✅ Expiry = request → version `expirationHours` → `forms.link.default-expiration-hours` (72). Bounded 1..`forms.template.max-expiration-hours` (8760), same as the template validator. |
| B9 | Template list's "sent to" count is hard-coded to `0L`. | `FormTemplateMapper.toListItem` | 🟡 `countByTemplateIds` query exists and `SentFormRepository` is injected in `FormTemplateService`, but not wired into the mapper. |
| B10 | `send-form-link` trusted a client-supplied `templateVersion` and didn't check the template is Active or that the version exists. | `SendFormLinkDto`, `FormEmailService` | ✅ `templateVersion` removed from the DTO; the server pins `currentVersion`. Missing template/version → 404, inactive template → 400. |

---

## 2. Design

### 2.1 Data model ✅

Schema is managed by Hibernate (`spring.jpa.hibernate.ddl-auto=update` in prod; Flyway is disabled), so the SQL below documents the target shape. All three entities already match it.

```sql
-- sent_forms: one row per form sent to a patient                       ✅
  id UUID PK, template_id UUID, template_version INT, status VARCHAR(20),
  sent_at, patient_id VARCHAR(64) NOT NULL, recipient_email VARCHAR(320),
  expires_at, opened_at, submitted_at, revoked_at, created_by
-- status: SENT | OPENED | IN_PROGRESS | SUBMITTED | EXPIRED | REVOKED

-- form_links                                                            ✅
  id UUID PK, token_hash UNIQUE NOT NULL, sent_form_id UUID NOT NULL,
  expires_at, used_at (set on SUBMIT), revoked_at, last_accessed_at,
  created_at NOT NULL

-- form_responses                                                        ✅
  id UUID PK, sent_form_id UUID NOT NULL UNIQUE, template_id UUID NOT NULL,
  template_version INT NOT NULL, answers JSONB NOT NULL DEFAULT '{}',
  status VARCHAR(20) NOT NULL,   -- DRAFT | SUBMITTED
  submitted_at, created_at NOT NULL, updated_at NOT NULL,
  lock_version BIGINT NOT NULL DEFAULT 0
```

Optional follow-ups: FK constraints (`sent_form_id → sent_forms.id`) are not declared on the entities; `FormResponse.status` could become an enum.

### 2.2 Answer format

| FieldType | Value |
|---|---|
| Tab, Document, FreeText | no answer (display only) |
| Input | `"text"`, or a `number` for the Number format |
| Dropdown, Radio | `"<optionId>"` |
| Checkbox | `["<optionId>", ...]` |
| Date | `"YYYY-MM-DD"` |
| Scale | `number` |
| Signature, Input (File) | `{ "fileName": "<s3 name>" }` |
| BodyDiagram | `[ { "pointId": "...", "note": "..." } ]` |
| Table | `[ { "<colId>": value, ... } ]` (one object per row) |

### 2.3 API

**Staff (JWT required)**

| Method | Path | Purpose | Status |
|---|---|---|---|
| POST | `/api/v1/form-templates/{id}/links` | `{patientId, email?, expiresInHours?}` → `FormLinkResponse`. Template must be Active. Expiry = request value → template `expirationHours` → default. | ⬜ (DTOs drafted) |
| POST | `/api/v1/emails/send-form-link` | Kept, but should delegate to the endpoint above. | 🟡 Exists standalone |
| GET | `/api/v1/sent-forms?templateId=&patientId=&status=` | List sent forms. | ⬜ |
| GET | `/api/v1/sent-forms/{id}/response` | Fields with answers. | ⬜ |
| POST | `/api/v1/sent-forms/{id}/revoke` | Revoke a link. | ⬜ |
| POST | `/api/v1/sent-forms/{id}/resend` | Issue a new token (old one revoked). Draft is kept. | ⬜ |

**Patient (public; token in the `X-Form-Token` header)**

| Method | Path | Purpose | Status |
|---|---|---|---|
| GET | `/api/public/forms` | Form definition (images presigned) + draft answers. SENT → OPENED on first open. | ⬜ |
| PUT | `/api/public/forms/draft` | `{answers, lockVersion}`. Autosave, status → IN_PROGRESS. `409` if `lockVersion` is stale. | ⬜ |
| POST | `/api/public/forms/submit` | `{answers}`. Validate, save as SUBMITTED, close link. `400 {errors:[{fieldId,message}]}` on failure. | ⬜ |
| POST | `/api/public/forms/uploads` | Presigned PUT under `responses/{sentFormId}/` for signatures and files. | ⬜ |

**Link errors:** `404 LINK_INVALID`, `410 LINK_EXPIRED`, `410 LINK_REVOKED`, `409 ALREADY_SUBMITTED`. ⬜ (requires B4)

### 2.4 Security ⬜
- A separate `SecurityFilterChain` with `permitAll` for `/api/public/forms/**`.
- Public responses contain only what's needed to fill the form — no patient data, no staff metadata — and are sent with `Cache-Control: no-store`.
- Rate limiting on the public endpoints (Redis is already a dependency).
- Patient uploads go to a separate `responses/` S3 prefix; type and size are checked on submit.
- CORS configured in one place for the frontend origin. (Currently `.cors(Customizer.withDefaults())` is enabled but no `CorsConfigurationSource` bean exists.)

---

## 3. Work plan

### Phase 0: Prerequisites (~0.5 day) — ⬜
- [ ] Add a global handler for `ApiException` (use its `status` and `errors`) and map link errors to 404/410/409. **(B4)**
- [ ] Add `src/main/resources/templates/form-request.html` (uses `${email.link}`). **(B1)**
- [ ] Add a central CORS config (`CorsConfigurationSource` bean) for the frontend origin.

### Phase 1: Data model (~1 day) — ✅
- [x] Extend `SentFormStatus` and switch `SentForm.status` to `@Enumerated(STRING)`. Add the new columns. **(B3)**
- [x] Rework `FormLink` to point to `sentFormId`. **(B2)**
- [x] Add the `FormResponse` entity and repository. **(B6, partial)**

### Phase 2: Link generation (~1 day) — 🟡
- [x] Create `SentForm` + `FormLink` and optionally email the link (currently in `FormEmailService.sendFormLink`).
- [x] Check the template is Active, pin the **current** version server-side, compute expiry (request → `expirationHours` → default). **(B8, B10)** — done in `FormEmailService.sendFormLink`.
- [ ] Extract that logic into `FormAssignmentService.createLink(...)` so the new `/links` endpoint can reuse it.
- [ ] Change `FormLinkService.validate()` to check revoked/expired (null-safe), stop rejecting on `usedAt`, update `lastAccessedAt`; set `usedAt` only on submit. **(B7)**
- [ ] Finish `FormLinkRequest` (add `patientId`, `email?`) and add `POST /form-templates/{id}/links`; make `send-form-link` delegate to it.
- [ ] Wire `countByTemplateIds` into the template list "sent to" count. **(B9)**

### Phase 3: Public form API (~2 days) — ⬜
- [ ] `PublicFormController`: GET, draft and submit, plus the public security chain. **(B5, B6)**
- [ ] Submit runs in a single transaction: response → SUBMITTED, `SentForm` → SUBMITTED, `link.usedAt` set. A repeated submit returns the existing result instead of an error.

### Phase 4: Answer validation (~1.5 days) — ⬜
- [ ] `FormResponseValidator` walks the pinned fields (can reuse `helper/FormFieldTraverser`) and checks:
  - required fields are present
  - answers have the right type/shape for each field type
  - option ids exist
  - `maxLength` and input formats are respected
  - table column ids are valid
  - uploaded files exist under the form's S3 prefix
- [ ] Re-enable the commented-out option checks in `FormTemplateValidator` (min options, duplicate labels/ids) so option ids can be trusted.

### Phase 5: Staff management (~1 day) — ⬜
- [ ] `SentFormController`: list, response, revoke and resend.
- [ ] Hourly job marking overdue forms as EXPIRED (needs `@EnableScheduling`; none exists yet).

### Phase 6: Uploads and tests (~1.5 days) — ⬜
- [ ] Presigned upload endpoint for signatures and files (can build on `S3PresignedUrlService`).
- [ ] Unit tests for the validator and link states (expired, revoked, used, invalid).
- [ ] Integration test: create link → open → draft → submit → staff reads the response.

**Remaining effort:** roughly 7–8 backend developer-days (Phase 1 and part of Phase 2 done).

---

## 4. Frontend contract

1. The `/forms/fill?token=…` page calls `GET /api/public/forms` with the `X-Form-Token` header.
2. It shows a friendly screen for each link error code.
3. It renders `fields` with the template builder's renderer in fill mode.
4. It autosaves with a debounced `PUT /draft` that sends `lockVersion`.
5. For signatures and files, it requests an upload URL, uploads to S3, and stores `{fileName}` in the answers.
6. On submit, it highlights `errors[].fieldId` on validation failure and shows a confirmation on success.

---

## 5. Open questions

1. ~~What type is the patient id?~~ **Resolved in code:** `String` (max 64 chars) on `SentForm.patientId`.
2. Where does each field's visible label live? `FormFieldDto.label` is still commented out, and the staff response view needs it.
3. Is having the link enough, or must the patient verify something first (date of birth or a one-time code)?
4. Can a patient edit a form after submitting it?
5. Should links be allowed to never expire? Current behaviour (B8): links always expire — default 72h, max 8760h. The `FormLinkRequest` javadoc ("null → never expires") contradicts this and should be updated once decided.
