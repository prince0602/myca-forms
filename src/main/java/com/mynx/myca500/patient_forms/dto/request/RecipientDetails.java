package com.mynx.myca500.patient_forms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;


public record RecipientDetails(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 320) String email,
        @Pattern(regexp = "^\\+?[0-9 ()-]{7,25}$", message = "must be a valid phone number") String phoneNumber,
        @Size(max = 500) String address,
        @Past LocalDate dob) {

    public RecipientDetails {
        phoneNumber = blankToNull(phoneNumber);
        address = blankToNull(address);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
