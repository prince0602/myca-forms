package com.mynx.myca500.patient_forms.mapper;

import com.mynx.myca500.patient_forms.dto.response.WorkspaceDetailsResponse;
import com.mynx.myca500.patient_forms.dto.response.WorkspaceSummaryResponse;
import com.mynx.myca500.patient_forms.entity.Membership;
import com.mynx.myca500.patient_forms.entity.Workspace;

public final class WorkspaceMapper {

    private WorkspaceMapper() {
    }

    public static WorkspaceSummaryResponse toSummary(Workspace workspace, Membership membership, boolean personal) {
        return new WorkspaceSummaryResponse(workspace.getWorkspaceId(), workspace.getName(), workspace.getPlan(),
                membership.getRole(), personal);
    }

    public static WorkspaceDetailsResponse toDetails(Workspace workspace, Membership membership) {
        return new WorkspaceDetailsResponse(workspace.getWorkspaceId(), workspace.getName(), workspace.getAccountType(),
                workspace.getPlan(), membership.getRole(), workspace.getFormsUsed(), workspace.getCustomMonthlyLimit(),
                workspace.getBaaVersion(), workspace.getBaaAcceptedAt());
    }
}
