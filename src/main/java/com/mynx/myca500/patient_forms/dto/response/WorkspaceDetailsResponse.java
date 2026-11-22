package com.mynx.myca500.patient_forms.dto.response;

import com.mynx.myca500.patient_forms.enums.AccountType;
import com.mynx.myca500.patient_forms.enums.MembershipRole;
import com.mynx.myca500.patient_forms.enums.Plan;

import java.time.Instant;

public record WorkspaceDetailsResponse(String workspaceId, String name, AccountType accountType, Plan plan,
                                       MembershipRole myRole, int formsUsed, Integer customMonthlyLimit,
                                       String baaVersion, Instant baaAcceptedAt) {
}
