package com.mynx.myca500.patient_forms.admin;

import com.mynx.myca500.patient_forms.entity.PlanLimit;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.entity.Workspace;
import com.mynx.myca500.patient_forms.entity.WorkspaceBonus;
import com.mynx.myca500.patient_forms.enums.AuditAction;
import com.mynx.myca500.patient_forms.enums.BillingInterval;
import com.mynx.myca500.patient_forms.enums.Plan;
import com.mynx.myca500.patient_forms.model.UsageSnapshot;
import com.mynx.myca500.patient_forms.repository.PlanLimitRepository;
import com.mynx.myca500.patient_forms.repository.UserRepository;
import com.mynx.myca500.patient_forms.repository.WorkspaceBonusRepository;
import com.mynx.myca500.patient_forms.repository.WorkspaceRepository;
import com.mynx.myca500.patient_forms.service.AuditService;
import com.mynx.myca500.patient_forms.service.FormUsageService;
import com.mynx.myca500.patient_forms.service.PlanChangeService;
import com.mynx.myca500.patient_forms.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class UsageAdminService {

    static final int MAX_REASON_LENGTH = 255;

    private final WorkspaceRepository workspaceRepository;
    private final PlanLimitRepository planLimitRepository;
    private final WorkspaceBonusRepository bonusRepository;
    private final UserRepository userRepository;
    private final FormUsageService formUsageService;
    private final PlanChangeService planChangeService;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AdminOperator resolveOperator(String email, String reason, String osUser, String host) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("--reason is required");
        }
        String trimmed = reason.trim();
        if (trimmed.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("--reason must be at most " + MAX_REASON_LENGTH + " characters");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("--operator=<email of your app user> is required");
        }
        User user = userRepository.findByEmail(EmailUtil.normalize(email))
                .orElseThrow(() -> new IllegalArgumentException("No user with email " + email));
        return new AdminOperator(user.getId(), user.getEmail(), osUser, host, trimmed);
    }

    @Transactional
    public String setPlanDefault(Plan plan, int limit, AdminOperator operator) {
        requireNonNegative(limit, "--limit");
        PlanLimit row = planLimitRepository.findById(plan)
                .orElseThrow(() -> new IllegalStateException("No plan_limits row for " + plan));
        int old = row.getMonthlyLimit();
        row.setMonthlyLimit(limit);
        row.setUpdatedAt(clock.instant());
        audit(AuditAction.PLAN_DEFAULT_LIMIT_CHANGED, operator, null, plan.name(),
                Map.of("plan", plan.name(), "oldValue", old, "newValue", limit));
        return "Default monthly limit for " + plan + ": " + old + " -> " + limit;
    }

    @Transactional
    public String setCustomLimit(String publicWorkspaceId, int limit, AdminOperator operator) {
        requireNonNegative(limit, "--limit");
        Workspace ws = lock(publicWorkspaceId);
        Integer old = ws.getCustomMonthlyLimit();
        ws.setCustomMonthlyLimit(limit);
        audit(AuditAction.CUSTOM_LIMIT_SET, operator, ws, ws.getWorkspaceId(),
                Map.of("oldValue", String.valueOf(old), "newValue", limit, "formsUsed", ws.getFormsUsed()));
        return "Custom monthly limit for " + ws.getWorkspaceId() + ": " + old + " -> " + limit;
    }

    @Transactional
    public String removeCustomLimit(String publicWorkspaceId, AdminOperator operator) {
        Workspace ws = lock(publicWorkspaceId);
        Integer old = ws.getCustomMonthlyLimit();
        if (old == null) {
            return "Workspace " + ws.getWorkspaceId() + " has no custom limit; nothing changed";
        }
        ws.setCustomMonthlyLimit(null);
        audit(AuditAction.CUSTOM_LIMIT_REMOVED, operator, ws, ws.getWorkspaceId(),
                Map.of("oldValue", old, "newValue", "plan default " + formUsageService.planDefault(ws.getPlan())));
        return "Custom monthly limit removed for " + ws.getWorkspaceId() + " (was " + old + ")";
    }

    @Transactional
    public String grantBonus(String publicWorkspaceId, int amount, AdminOperator operator) {
        if (amount <= 0) {
            throw new IllegalArgumentException("--amount must be a positive whole number");
        }
        Workspace ws = lock(publicWorkspaceId);
        long before = bonusRepository.sumRemaining(ws.getId());

        WorkspaceBonus bonus = new WorkspaceBonus();
        bonus.setWorkspaceId(ws.getId());
        bonus.setAmount(amount);
        bonus.setFormsUsed(0);
        bonus.setReason(operator.reason());
        bonus.setGrantedBy(operator.userId());
        bonus.setGrantedAt(clock.instant());
        bonusRepository.save(bonus);

        audit(AuditAction.BONUS_GRANTED, operator, ws, ws.getWorkspaceId(),
                Map.of("bonusId", bonus.getId().toString(), "amount", amount,
                        "oldValue", before, "newValue", before + amount));
        return "Granted " + amount + " bonus forms to " + ws.getWorkspaceId()
                + " (bonus remaining " + before + " -> " + (before + amount) + ")";
    }

    @Transactional
    public String setPlan(String publicWorkspaceId, Plan plan, BillingInterval interval, AdminOperator operator) {
        Workspace ws = lock(publicWorkspaceId);
        if (ws.getPlan() == plan) {
            return "Workspace " + ws.getWorkspaceId() + " is already on " + plan + "; nothing changed";
        }
        Map<String, Object> details = new HashMap<>(planChangeService.changePlanLocked(ws, plan, interval));
        details.put("interval", String.valueOf(interval));
        audit(AuditAction.PLAN_CHANGED, operator, ws, ws.getWorkspaceId(), details);
        return "Plan for " + ws.getWorkspaceId() + ": " + details.get("oldPlan") + " -> " + plan;
    }

    @Transactional(readOnly = true)
    public String show(String publicWorkspaceId) {
        Workspace ws = workspaceRepository.findByWorkspaceId(publicWorkspaceId)
                .orElseThrow(() -> new IllegalArgumentException("No workspace " + publicWorkspaceId));
        UsageSnapshot u = formUsageService.snapshot(ws);
        return String.format("%s plan=%s customLimit=%s limit=%d used=%d bonusRemaining=%d refreshesAt=%s",
                ws.getWorkspaceId(), u.plan(), ws.getCustomMonthlyLimit(), u.monthlyLimit(), u.formsUsed(),
                u.bonusFormsRemaining(), u.refreshAt());
    }

    private Workspace lock(String publicWorkspaceId) {
        Workspace ws = workspaceRepository.findByWorkspaceId(publicWorkspaceId)
                .orElseThrow(() -> new IllegalArgumentException("No workspace " + publicWorkspaceId));
        return formUsageService.lock(ws.getId());
    }

    private void audit(AuditAction action, AdminOperator operator, Workspace ws, String target,
                       Map<String, Object> change) {
        Map<String, Object> details = new HashMap<>(change);
        details.put("reason", operator.reason());
        details.put("source", "usage-admin");
        details.put("operatorEmail", operator.email());
        details.put("osUser", String.valueOf(operator.osUser()));
        details.put("host", String.valueOf(operator.host()));
        details.put("at", Instant.now(clock).toString());
        auditService.record(action, operator.userId(), ws == null ? null : ws.getId(), target, details);
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be a whole number of 0 or more");
        }
    }
}
