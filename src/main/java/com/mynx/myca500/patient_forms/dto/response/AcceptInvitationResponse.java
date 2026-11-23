package com.mynx.myca500.patient_forms.dto.response;

import com.mynx.myca500.patient_forms.enums.MembershipRole;

import java.time.Instant;

public record AcceptInvitationResponse(
        InvitationPreviewResponse.WorkspaceRef workspace,
        MembershipRole role,
        Instant joinedAt) {
}
