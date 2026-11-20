package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.enums.OnboardingStatus;
import com.mynx.myca500.patient_forms.exception.ApiException;
import com.mynx.myca500.patient_forms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final int ACCOUNT_ID_LETTERS = 2;
    private static final int ACCOUNT_ID_DIGITS = 10;

    private final UserRepository users;
    private final SecureRandom random = new SecureRandom();

    public User registerIfAbsent(Jwt jwt) {
        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        return users.findByKeycloakUserId(keycloakUserId)
                .orElseGet(() -> create(keycloakUserId, jwt));
    }

    private User create(UUID keycloakUserId, Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (!StringUtils.hasText(email)) {
            throw ApiException.badRequest(List.of("Authenticated token has no email claim"));
        }

        User user = new User();
        user.setKeycloakUserId(keycloakUserId);
        user.setAccountId(generateAccountId());
        user.setEmail(email.trim().toLowerCase());
        user.setFirstName(jwt.getClaimAsString("given_name"));
        user.setLastName(jwt.getClaimAsString("family_name"));
        user.setOnboardingStatus(OnboardingStatus.PROFILE_REQUIRED);

        try {
            User saved = users.saveAndFlush(user);
            log.info("Registered user {} for Keycloak subject {}", saved.getId(), keycloakUserId);
            return saved;
        } catch (DataIntegrityViolationException e) {
            return users.findByKeycloakUserId(keycloakUserId)
                    .orElseThrow(() -> ApiException.conflict("A user with this email is already registered"));
        }
    }

    private String generateAccountId() {
        StringBuilder sb = new StringBuilder(ACCOUNT_ID_LETTERS + ACCOUNT_ID_DIGITS);
        for (int i = 0; i < ACCOUNT_ID_LETTERS; i++) {
            sb.append((char) ('A' + random.nextInt(26)));
        }
        for (int i = 0; i < ACCOUNT_ID_DIGITS; i++) {
            sb.append((char) ('0' + random.nextInt(10)));
        }
        return sb.toString();
    }

    public static String formatAccountId(String accountId) {
        if (accountId == null || accountId.length() != ACCOUNT_ID_LETTERS + ACCOUNT_ID_DIGITS) {
            return accountId;
        }
        return accountId.substring(0, 2) + " " + accountId.substring(2, 6) + " "
                + accountId.substring(6, 10) + " " + accountId.substring(10);
    }
}
