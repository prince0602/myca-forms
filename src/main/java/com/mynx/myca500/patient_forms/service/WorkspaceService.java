package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.response.WorkspaceDetailsResponse;
import com.mynx.myca500.patient_forms.dto.response.WorkspaceSummaryResponse;
import com.mynx.myca500.patient_forms.entity.BaaDocument;
import com.mynx.myca500.patient_forms.entity.Membership;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.entity.Workspace;
import com.mynx.myca500.patient_forms.enums.*;
import com.mynx.myca500.patient_forms.mapper.WorkspaceMapper;
import com.mynx.myca500.patient_forms.model.ClientInfo;
import com.mynx.myca500.patient_forms.model.WorkspaceAccess;
import com.mynx.myca500.patient_forms.repository.MembershipRepository;
import com.mynx.myca500.patient_forms.repository.WorkspaceRepository;
import com.mynx.myca500.patient_forms.util.BillingCycleUtil;
import com.mynx.myca500.patient_forms.util.PublicIdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private static final int MAX_ID_ATTEMPTS = 5;

    private final WorkspaceRepository workspaceRepository;
    private final MembershipRepository membershipRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public Workspace createPersonalWorkspace(User owner, String name, AccountType accountType,
                                             BaaDocument baa, ClientInfo client) {
        Instant now = clock.instant();

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(generateUniqueWorkspaceId());
        workspace.setName(name);
        workspace.setAccountType(accountType);
        workspace.setPlan(Plan.FREE);
        workspace.setFormsUsed(0);
        workspace.setBillingAnchorAt(now);
        workspace.setUsageRefreshAt(BillingCycleUtil.nextRefreshAfter(now, now));
        workspace.setBaaVersion(baa.getVersion());
        workspace.setBaaAcceptedBy(owner.getId());
        workspace.setBaaAcceptedAt(now);
        workspace.setBaaAcceptedIp(client.ip());
        workspace.setBaaAcceptedUserAgent(client.userAgent());
        workspace.setCreatedBy(owner.getId());
        workspaceRepository.save(workspace);

        Membership ownerMembership = new Membership();
        ownerMembership.setUserId(owner.getId());
        ownerMembership.setWorkspaceId(workspace.getId());
        ownerMembership.setRole(MembershipRole.OWNER);
        ownerMembership.setStatus(MembershipStatus.ACTIVE);
        ownerMembership.setJoinedAt(now);
        membershipRepository.save(ownerMembership);

        auditService.record(AuditAction.WORKSPACE_CREATED, owner.getId(), workspace.getId(), workspace.getWorkspaceId(),
                Map.of("accountType", accountType.name(), "plan", Plan.FREE.name()));
        auditService.record(AuditAction.BAA_ACCEPTED, owner.getId(), workspace.getId(), workspace.getWorkspaceId(),
                Map.of("version", baa.getVersion()));
        return workspace;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceSummaryResponse> listAccessibleWorkspaces(User user) {
        List<Membership> memberships = membershipRepository.findAccessibleByUserId(
                user.getId(), MembershipStatus.ACTIVE, clock.instant());

        Map<UUID, Workspace> workspacesById = workspaceRepository
                .findAllById(memberships.stream().map(Membership::getWorkspaceId).toList())
                .stream()
                .collect(Collectors.toMap(Workspace::getId, Function.identity()));

        return memberships.stream()
                .map(m -> WorkspaceMapper.toSummary(workspacesById.get(m.getWorkspaceId()), m,
                        Objects.equals(m.getWorkspaceId(), user.getPersonalWorkspaceId())))
                .sorted(Comparator.comparing((WorkspaceSummaryResponse s) -> !s.personal())
                        .thenComparing(WorkspaceSummaryResponse::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<String> findLandingWorkspaceId(User user) {
        for (UUID candidate : new UUID[]{user.getDefaultWorkspaceId(), user.getPersonalWorkspaceId()}) {
            if (candidate == null) {
                continue;
            }
            boolean accessible = membershipRepository.findByUserIdAndWorkspaceId(user.getId(), candidate)
                    .map(workspaceAccessService::hasAccess)
                    .orElse(false);
            if (accessible) {
                return workspaceRepository.findById(candidate).map(Workspace::getWorkspaceId);
            }
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public WorkspaceDetailsResponse getWorkspaceDetails(User user, String publicWorkspaceId) {
        WorkspaceAccess access = workspaceAccessService.requireMember(user.getId(), publicWorkspaceId);
        return WorkspaceMapper.toDetails(access.workspace(), access.membership());
    }

    private String generateUniqueWorkspaceId() {
        for (int i = 0; i < MAX_ID_ATTEMPTS; i++) {
            String candidate = PublicIdUtil.newWorkspaceId();
            if (!workspaceRepository.existsByWorkspaceId(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique Workspace ID");
    }
}
