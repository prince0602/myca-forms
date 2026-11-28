package com.mynx.myca500.patient_forms.payment;

import com.mynx.myca500.patient_forms.exception.BusinessException;
import com.mynx.myca500.patient_forms.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!prod")
@ConditionalOnProperty(name = "billing.payment-gateway", havingValue = "fake")
public class FakePaymentGateway implements PaymentGateway {

    private static final String PREFIX = "fake_";

    @PostConstruct
    void warn() {
        log.warn("FakePaymentGateway is active: card payments are NOT charged. Never enable this outside development.");
    }

    @Override
    public VerifiedPayment verify(PaymentVerificationRequest request) {
        if (request.paymentReference() == null || !request.paymentReference().startsWith(PREFIX)) {
            throw new BusinessException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
        return new VerifiedPayment(request.paymentReference(), request.expectedAmountCents(), request.currency());
    }

    @Override
    public void refund(String paymentReference) {
        log.info("Fake refund for {}", paymentReference);
    }
}
