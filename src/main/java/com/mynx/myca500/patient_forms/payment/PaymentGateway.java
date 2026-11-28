package com.mynx.myca500.patient_forms.payment;


public interface PaymentGateway {

    VerifiedPayment verify(PaymentVerificationRequest request);

    void refund(String paymentReference);
}
