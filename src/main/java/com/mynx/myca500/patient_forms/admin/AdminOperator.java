package com.mynx.myca500.patient_forms.admin;

import java.util.UUID;


public record AdminOperator(UUID userId, String email, String osUser, String host, String reason) {
}
