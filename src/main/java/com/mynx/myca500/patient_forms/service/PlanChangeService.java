package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.entity.Workspace;
import com.mynx.myca500.patient_forms.entity.WorkspacePayment;
import com.mynx.myca500.patient_forms.enums.AuditAction;
import com.mynx.myca500.patient_forms.enums.BillingInterval;
import com.mynx.myca500.patient_forms.enums.Plan;
import com.mynx.myca500.patient_forms.exception.BusinessException;
import com.mynx.myca500.patient_forms.exception.ErrorCode;
import com.mynx.myca500.patient_forms.payment.VerifiedPayment;
import com.mynx.myca500.patient_forms.repository.WorkspacePaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class PlanChangeService {

    private final FormUsageService formUsageService;
    private final WorkspacePaymentRepository paymentRepository;
    private final AuditService auditService;
    private final Clock clock;


    @Transactional
    public Workspace applyPaidUpgrade(UUID workspaceId, Plan target, BillingInterval interval,
                                      VerifiedPayment payment, UUID paidBy) {
        Workspace workspace = formUsageService.lock(workspaceId);
        if (workspace.getPlan() == target) {
            throw new BusinessException(ErrorCode.ALREADY_ON_PLAN);
        }
        Instant now = clock.instant();

        WorkspacePayment record = new WorkspacePayment();
        record.setWorkspaceId(workspaceId);
        record.setPaymentReference(payment.paymentReference());
        record.setPlan(target);
        record.setBillingInterval(interval);
        record.setAmountCents(payment.amountCents());
        record.setCurrency(payment.currency());
        record.setPaidBy(paidBy);
        record.setPaidAt(now);
        try {
            paymentRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_USED);
        }

        Plan from = workspace.getPlan();
        applyPlan(workspace, target, interval, now);
        workspace.setPaymentReference(payment.paymentReference());

        auditService.record(AuditAction.PLAN_UPGRADED, paidBy, workspaceId, workspace.getWorkspaceId(), Map.of(
                "fromPlan", from.name(), "toPlan", target.name(), "interval", interval.name(),
                "amountCents", payment.amountCents(), "currency", payment.currency(),
                "paymentReference", payment.paymentReference()));
        return workspace;
    }


    @Transactional(propagation = Propagation.MANDATORY)
    public Map<String, Object> changePlanLocked(Workspace workspace, Plan target, BillingInterval interval) {
        Map<String, Object> details = new HashMap<>();
        details.put("oldPlan", workspace.getPlan().name());
        details.put("oldCustomMonthlyLimit", String.valueOf(workspace.getCustomMonthlyLimit()));
        applyPlan(workspace, target, interval, clock.instant());
        details.put("newPlan", target.name());
        details.put("newCustomMonthlyLimit", String.valueOf(workspace.getCustomMonthlyLimit()));
        return details;
    }

    private void applyPlan(Workspace workspace, Plan target, BillingInterval interval, Instant now) {
        Plan from = workspace.getPlan();
        workspace.setPlan(target);
        if (target == Plan.FREE) {
            workspace.setCustomMonthlyLimit(null);
            workspace.setBillingInterval(null);
            formUsageService.rolloverIfDue(workspace, now);
        } else {
            workspace.setBillingInterval(interval);
            if (from != target) {
                formUsageService.startNewCycle(workspace, now);
            }
        }
    }
}
