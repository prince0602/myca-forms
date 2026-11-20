package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.entity.Membership;
import com.mynx.myca500.patient_forms.entity.Workspace;
import com.mynx.myca500.patient_forms.enums.MembershipStatus;
import com.mynx.myca500.patient_forms.exception.BusinessException;
import com.mynx.myca500.patient_forms.exception.ErrorCode;
import com.mynx.myca500.patient_forms.model.WorkspaceAccess;
import com.mynx.myca500.patient_forms.repository.MembershipRepository;
import com.mynx.myca500.patient_forms.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceAccessService {

    private final WorkspaceRepository workspaceRepository;
    private final MembershipRepository membershipRepository;
    private final Clock clock;

    public WorkspaceAccess requireMember(UUID userId, String publicWorkspaceId) {
        Workspace workspace = workspaceRepository.findByWorkspaceId(publicWorkspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));
        Membership membership = membershipRepository.findByUserIdAndWorkspaceId(userId, workspace.getId())
                .filter(this::hasAccess)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));
        return new WorkspaceAccess(workspace, membership);
    }

    public WorkspaceAccess requireOwner(UUID userId, String publicWorkspaceId) {
        WorkspaceAccess access = requireMember(userId, publicWorkspaceId);
        if (!access.isOwner()) {
            throw new BusinessException(ErrorCode.NOT_WORKSPACE_OWNER);
        }
        return access;
    }

    public boolean hasAccess(Membership membership) {
        return membership.getStatus() == MembershipStatus.ACTIVE
                && (membership.getAccessEndsAt() == null || membership.getAccessEndsAt().isAfter(clock.instant()));
    }
}
