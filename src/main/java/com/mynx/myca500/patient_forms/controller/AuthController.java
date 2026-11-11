package com.mynx.myca500.patient_forms.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtDecoder jwtDecoder;

    public AuthController(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }


    @GetMapping("/validate")
    public TokenValidationResponse validate(@AuthenticationPrincipal Jwt jwt) {
        return TokenValidationResponse.valid(jwt);
    }


    @GetMapping("/me")
    public UserInfo me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
        return new UserInfo(
                jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name"),
                authorities);
    }


    @PostMapping("/validate-token")
    public ResponseEntity<TokenValidationResponse> validateToken(@Valid @RequestBody TokenRequest request) {
        try {
            Jwt jwt = jwtDecoder.decode(request.token());
            return ResponseEntity.ok(TokenValidationResponse.valid(jwt));
        } catch (JwtException e) {
            return ResponseEntity.ok(TokenValidationResponse.invalid("Invalid or expired token"));
        }
    }

    public record TokenRequest(@NotBlank String token) {}

    public record TokenValidationResponse(
            boolean valid,
            String subject,
            String username,
            Instant expiresAt,
            String message) {

        static TokenValidationResponse valid(Jwt jwt) {
            return new TokenValidationResponse(
                    true,
                    jwt.getSubject(),
                    jwt.getClaimAsString("preferred_username"),
                    jwt.getExpiresAt(),
                    null);
        }

        static TokenValidationResponse invalid(String message) {
            return new TokenValidationResponse(false, null, null, null, message);
        }
    }

    public record UserInfo(
            String subject,
            String username,
            String email,
            String name,
            List<String> roles) {}
}
