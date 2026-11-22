package com.mynx.myca500.patient_forms.dto.response;

import com.mynx.myca500.patient_forms.enums.MembershipRole;
import com.mynx.myca500.patient_forms.enums.Plan;

public record WorkspaceSummaryResponse(String workspaceId, String name, Plan plan,
                                       MembershipRole role, boolean personal) {
}
