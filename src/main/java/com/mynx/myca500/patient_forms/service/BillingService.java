package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.request.UpgradePlanRequest;
import com.mynx.myca500.patient_forms.dto.response.WorkspaceUsageResponse;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.entity.Workspace;
import com.mynx.myca500.patient_forms.entity.WorkspacePayment;
import com.mynx.myca500.patient_forms.enums.Plan;
import com.mynx.myca500.patient_forms.exception.BusinessException;
import com.mynx.myca500.patient_forms.exception.ErrorCode;
import com.mynx.myca500.patient_forms.model.PlanPrice;
import com.mynx.myca500.patient_forms.model.WorkspaceAccess;
import com.mynx.myca500.patient_forms.payment.PaymentGateway;
import com.mynx.myca500.patient_forms.payment.PaymentVerificationRequest;
import com.mynx.myca500.patient_forms.payment.VerifiedPayment;
import com.mynx.myca500.patient_forms.repository.WorkspacePaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final WorkspaceAccessService workspaceAccessService;
    private final FormUsageService formUsageService;
    private final PlanChangeService planChangeService;
    private final WorkspacePaymentRepository paymentRepository;
    private final ObjectProvider<PaymentGateway> paymentGateway;
    private final Clock clock;

    @Transactional(readOnly = true)
    public WorkspaceUsageResponse getUsage(User user, String publicWorkspaceId) {
        Workspace workspace = workspaceAccessService.requireMember(user.getId(), publicWorkspaceId).workspace();
        return toResponse(workspace);
    }

    public WorkspaceUsageResponse upgrade(User owner, String publicWorkspaceId, UpgradePlanRequest request) {
        WorkspaceAccess access = workspaceAccessService.requireOwner(owner.getId(), publicWorkspaceId);
        Workspace workspace = access.workspace();
        PlanPrice price = PlanPrice.of(request.plan(), request.billingInterval())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_PURCHASABLE));
        String reference = request.paymentReference().trim();

        Optional<WorkspacePayment> existing = paymentRepository.findByPaymentReference(reference);
        if (existing.isPresent()) {
            if (!existing.get().getWorkspaceId().equals(workspace.getId())) {
                throw new BusinessException(ErrorCode.PAYMENT_ALREADY_USED);
            }
            return toResponse(workspace);
        }
        if (workspace.getPlan() == price.plan()) {
            throw new BusinessException(ErrorCode.ALREADY_ON_PLAN);
        }

        PaymentGateway gateway = paymentGateway.getIfAvailable();
        if (gateway == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_CONFIGURED);
        }
        VerifiedPayment payment = gateway.verify(new PaymentVerificationRequest(
                reference, price.chargeCents(), price.currency(), workspace.getWorkspaceId()));

        try {
            Workspace upgraded = planChangeService.applyPaidUpgrade(workspace.getId(), price.plan(),
                    price.interval(), payment, owner.getId());
            return toResponse(upgraded);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.ALREADY_ON_PLAN) {
                refundQuietly(gateway, reference);
            }
            throw e;
        }
    }

    private WorkspaceUsageResponse toResponse(Workspace workspace) {
        int plusDefault = workspace.getPlan() == Plan.FREE ? formUsageService.planDefault(Plan.PLUS) : 0;
        return WorkspaceUsageResponse.from(formUsageService.snapshot(workspace), plusDefault, clock.instant());
    }

    private static void refundQuietly(PaymentGateway gateway, String reference) {
        try {
            gateway.refund(reference);
            log.warn("Refunded duplicate upgrade payment {}", reference);
        } catch (RuntimeException ex) {
            log.error("Refund failed for duplicate upgrade payment {}; reconcile manually", reference, ex);
        }
    }
}
