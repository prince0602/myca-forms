package com.mynx.myca500.patient_forms.dto.response;

import com.mynx.myca500.patient_forms.enums.MembershipRole;

import java.time.Instant;

public record MemberResponse(String accountId, String name, String email, MembershipRole role,
                             Instant joinedAt, Instant accessEndsAt) {
}
