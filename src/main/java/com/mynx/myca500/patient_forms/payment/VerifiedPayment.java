package com.mynx.myca500.patient_forms.payment;

public record VerifiedPayment(String paymentReference, long amountCents, String currency) {
}
