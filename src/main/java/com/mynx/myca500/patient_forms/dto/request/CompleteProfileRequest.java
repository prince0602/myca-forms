package com.mynx.myca500.patient_forms.dto.request;

import com.mynx.myca500.patient_forms.enums.Profession;
import com.mynx.myca500.patient_forms.enums.TeamSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompleteProfileRequest(
        @Size(max = 200) String businessName,
        @NotNull Profession profession,
        @Size(max = 100) String professionOther,
        @NotNull TeamSize teamSize,
        @NotBlank @Size(max = 25) String phone,
        @NotBlank @Size(max = 20) String baaVersion,
        boolean baaAccepted) {
}
