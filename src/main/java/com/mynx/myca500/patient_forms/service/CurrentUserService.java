package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.exception.BusinessException;
import com.mynx.myca500.patient_forms.exception.ErrorCode;
import com.mynx.myca500.patient_forms.repository.UserRepository;
import com.mynx.myca500.patient_forms.security.KeycloakClaims;
import com.mynx.myca500.patient_forms.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private static final int MAX_PROVISION_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final UserProvisioningService userProvisioningService;

    @Transactional
    public User resolve(Jwt jwt) {
        KeycloakClaims claims = KeycloakClaims.from(jwt);
        if (!claims.emailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        return userRepository.findByKeycloakUserId(claims.keycloakUserId())
                .map(user -> syncWithKeycloak(user, claims))
                .orElseGet(() -> provision(claims));
    }


    private User syncWithKeycloak(User user, KeycloakClaims claims) {
        String email = EmailUtil.normalize(claims.email());
        if (!email.equals(user.getEmail())) {
            user.setEmail(email);
        }
        if (claims.firstName() != null && !Objects.equals(claims.firstName(), user.getFirstName())) {
            user.setFirstName(claims.firstName());
        }
        if (claims.lastName() != null && !Objects.equals(claims.lastName(), user.getLastName())) {
            user.setLastName(claims.lastName());
        }
        return user;
    }


    private User provision(KeycloakClaims claims) {
        for (int attempt = 1; ; attempt++) {
            try {
                UUID userId = userProvisioningService.createUser(claims);
                log.info("Provisioned new user for keycloak sub {}", claims.keycloakUserId());
                return userRepository.findById(userId).orElseThrow();
            } catch (DataIntegrityViolationException e) {
                Optional<User> createdByOtherRequest = userRepository.findByKeycloakUserId(claims.keycloakUserId());
                if (createdByOtherRequest.isPresent()) {
                    return createdByOtherRequest.get();
                }
                if (attempt >= MAX_PROVISION_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }
}
