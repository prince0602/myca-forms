package com.mynx.myca500.patient_forms.mapper;

import com.mynx.myca500.patient_forms.dto.response.MemberResponse;
import com.mynx.myca500.patient_forms.entity.Membership;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.util.NameUtil;
import com.mynx.myca500.patient_forms.util.PublicIdUtil;

public final class MemberMapper {

    private MemberMapper() {
    }

    public static MemberResponse toResponse(Membership membership, User user) {
        return new MemberResponse(PublicIdUtil.formatAccountId(user.getAccountId()),
                NameUtil.fullName(user.getFirstName(), user.getLastName()), user.getEmail(),
                membership.getRole(), membership.getJoinedAt(), membership.getAccessEndsAt());
    }
}
