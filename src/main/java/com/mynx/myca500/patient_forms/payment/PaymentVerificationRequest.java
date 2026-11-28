package com.mynx.myca500.patient_forms.payment;

public record PaymentVerificationRequest(String paymentReference, long expectedAmountCents, String currency,
                                         String workspaceId) {
}
