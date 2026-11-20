package com.mynx.myca500.patient_forms.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public record KeycloakClaims(UUID keycloakUserId, String email, boolean emailVerified,
                             String firstName, String lastName) {

    public static KeycloakClaims from(Jwt jwt) {
        return new KeycloakClaims(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"));
    }
}
