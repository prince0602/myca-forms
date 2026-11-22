package com.mynx.myca500.patient_forms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Body for POST /api/workspaces/{workspaceId}/patients. All fields are mandatory. dob is ISO yyyy-MM-dd. */
public record OnboardPatientRequest(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{7,25}$", message = "must be a valid phone number")
        String phoneNumber,
        @NotNull @Past LocalDate dob) {
}
