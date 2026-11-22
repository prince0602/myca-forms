package com.mynx.myca500.patient_forms.dto.response;

import com.mynx.myca500.patient_forms.entity.Patient;

import java.time.Instant;
import java.time.LocalDate;

/** workspaceId is the public 6-character workspace ID. */
public record PatientResponse(
        String id,
        String workspaceId,
        String fullName,
        String address,
        String email,
        String phoneNumber,
        LocalDate dob,
        Instant createdAt) {

    public static PatientResponse from(Patient p, String publicWorkspaceId) {
        return new PatientResponse(p.getId().toString(), publicWorkspaceId, p.getFullName(), p.getAddress(),
                p.getEmail(), p.getPhoneNumber(), p.getDob(), p.getCreatedAt());
    }
}
