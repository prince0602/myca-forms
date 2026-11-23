package com.mynx.myca500.patient_forms.dto.response;

import java.time.Instant;


public record InvitationPreviewResponse(
        String status,
        WorkspaceRef workspace,
        InviterRef invitedBy,
        String email,
        boolean accountExists,
        Instant expiresAt) {

    public record WorkspaceRef(String workspaceId, String name) {
    }

    public record InviterRef(String name) {
    }
}
