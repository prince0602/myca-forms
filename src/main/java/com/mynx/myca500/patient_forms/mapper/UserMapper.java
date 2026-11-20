package com.mynx.myca500.patient_forms.mapper;

import com.mynx.myca500.patient_forms.dto.response.MeResponse;
import com.mynx.myca500.patient_forms.dto.response.WorkspaceSummaryResponse;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.util.PublicIdUtil;

import java.util.List;

public final class UserMapper {

    private UserMapper() {
    }

    public static MeResponse toMe(User user, List<WorkspaceSummaryResponse> workspaces, String currentWorkspaceId) {
        return new MeResponse(PublicIdUtil.formatAccountId(user.getAccountId()), user.getFirstName(),
                user.getLastName(), user.getEmail(), user.getOnboardingStatus(), workspaces, currentWorkspaceId);
    }
}
