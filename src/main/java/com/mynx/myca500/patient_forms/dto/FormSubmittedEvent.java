package com.mynx.myca500.patient_forms.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Published inside the submit transaction. Consume it with
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} (+ {@code @Async}) so that
 * notifications / PDF / EHR sync never slow down or fail the patient's request.
 */
public record FormSubmittedEvent(UUID sentFormId, UUID formResponseId, Instant submittedAt) {
}
