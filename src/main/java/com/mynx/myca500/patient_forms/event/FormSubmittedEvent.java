package com.mynx.myca500.patient_forms.event;

import java.time.Instant;
import java.util.UUID;

public record FormSubmittedEvent(UUID sentFormId, UUID formResponseId, Instant submittedAt) {
}
