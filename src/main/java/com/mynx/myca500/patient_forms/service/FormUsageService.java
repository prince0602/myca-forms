package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.entity.PlanLimit;
import com.mynx.myca500.patient_forms.entity.Workspace;
import com.mynx.myca500.patient_forms.entity.WorkspaceBonus;
import com.mynx.myca500.patient_forms.enums.Plan;
import com.mynx.myca500.patient_forms.exception.BusinessException;
import com.mynx.myca500.patient_forms.exception.ErrorCode;
import com.mynx.myca500.patient_forms.model.UsageSnapshot;
import com.mynx.myca500.patient_forms.repository.PlanLimitRepository;
import com.mynx.myca500.patient_forms.repository.WorkspaceBonusRepository;
import com.mynx.myca500.patient_forms.repository.WorkspaceRepository;
import com.mynx.myca500.patient_forms.util.BillingCycleUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class FormUsageService {

    private static final PageRequest FIRST = PageRequest.of(0, 1);

    private final WorkspaceRepository workspaceRepository;
    private final PlanLimitRepository planLimitRepository;
    private final WorkspaceBonusRepository bonusRepository;
    private final Clock clock;


    @Transactional(propagation = Propagation.MANDATORY)
    public void consumeOne(UUID workspaceId) {
        Workspace workspace = lock(workspaceId);
        rolloverIfDue(workspace, clock.instant());

        if (workspace.getFormsUsed() < monthlyLimit(workspace)) {
            workspace.setFormsUsed(workspace.getFormsUsed() + 1);
            return;
        }
        WorkspaceBonus bonus = bonusRepository.findAvailable(workspaceId, FIRST).stream().findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.FORM_LIMIT_REACHED));
        bonus.setFormsUsed(bonus.getFormsUsed() + 1);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Workspace lock(UUID workspaceId) {
        return workspaceRepository.lockById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));
    }

    void rolloverIfDue(Workspace workspace, Instant now) {
        if (BillingCycleUtil.isRefreshDue(workspace.getUsageRefreshAt(), now)) {
            workspace.setFormsUsed(0);
            workspace.setUsageRefreshAt(BillingCycleUtil.nextRefreshAfter(workspace.getBillingAnchorAt(), now));
        }
    }
    void startNewCycle(Workspace workspace, Instant now) {
        workspace.setBillingAnchorAt(now);
        workspace.setFormsUsed(0);
        workspace.setUsageRefreshAt(BillingCycleUtil.nextRefreshAfter(now, now));
    }

    public int monthlyLimit(Workspace workspace) {
        Integer custom = workspace.getCustomMonthlyLimit();
        return custom != null ? custom : planDefault(workspace.getPlan());
    }

    public int planDefault(Plan plan) {
        return planLimitRepository.findById(plan)
                .map(PlanLimit::getMonthlyLimit)
                .orElseThrow(() -> new IllegalStateException("No plan_limits row for plan " + plan));
    }

    @Transactional(readOnly = true)
    public UsageSnapshot snapshot(Workspace workspace) {
        Instant now = clock.instant();
        boolean due = BillingCycleUtil.isRefreshDue(workspace.getUsageRefreshAt(), now);
        Instant refreshAt = due
                ? BillingCycleUtil.nextRefreshAfter(workspace.getBillingAnchorAt(), now)
                : workspace.getUsageRefreshAt();
        return new UsageSnapshot(workspace.getPlan(), workspace.getBillingInterval(), monthlyLimit(workspace),
                workspace.getCustomMonthlyLimit() != null, due ? 0 : workspace.getFormsUsed(),
                bonusRepository.sumRemaining(workspace.getId()), refreshAt);
    }
}
