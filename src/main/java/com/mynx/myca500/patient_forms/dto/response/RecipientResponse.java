package com.mynx.myca500.patient_forms.dto.response;

import com.mynx.myca500.patient_forms.entity.Recipient;

import java.time.Instant;
import java.time.LocalDate;

public record RecipientResponse(
        String id,
        String workspaceId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String address,
        LocalDate dob,
        Instant createdAt) {

    public static RecipientResponse from(Recipient r, String publicWorkspaceId) {
        return new RecipientResponse(r.getId().toString(), publicWorkspaceId, r.getFirstName(), r.getLastName(),
                r.getEmail(), r.getPhoneNumber(), r.getAddress(), r.getDob(), r.getCreatedAt());
    }
}
