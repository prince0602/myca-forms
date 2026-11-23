package com.mynx.myca500.patient_forms.dto.response;

import com.mynx.myca500.patient_forms.enums.MembershipRole;

import java.time.Instant;

public record InvitationResponse(
        String id,
        String email,
        MembershipRole role,
        String status,
        Inviter invitedBy,
        Instant createdAt,
        Instant expiresAt) {

    public record Inviter(String accountId, String name) {
    }
}
