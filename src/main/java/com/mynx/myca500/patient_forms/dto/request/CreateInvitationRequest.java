package com.mynx.myca500.patient_forms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInvitationRequest(@NotBlank @Email @Size(max = 320) String email) {
}
