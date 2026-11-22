package com.mynx.myca500.patient_forms.model;

import com.mynx.myca500.patient_forms.entity.Membership;
import com.mynx.myca500.patient_forms.entity.Workspace;
import com.mynx.myca500.patient_forms.enums.MembershipRole;

public record WorkspaceAccess(Workspace workspace, Membership membership) {

    public boolean isOwner() {
        return membership.getRole() == MembershipRole.OWNER;
    }
}
