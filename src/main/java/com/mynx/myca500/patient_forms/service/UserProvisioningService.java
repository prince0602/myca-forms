package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.enums.AuditAction;
import com.mynx.myca500.patient_forms.enums.OnboardingStatus;
import com.mynx.myca500.patient_forms.repository.UserRepository;
import com.mynx.myca500.patient_forms.security.KeycloakClaims;
import com.mynx.myca500.patient_forms.util.EmailUtil;
import com.mynx.myca500.patient_forms.util.PublicIdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID createUser(KeycloakClaims claims) {
        User user = new User();
        user.setKeycloakUserId(claims.keycloakUserId());
        user.setAccountId(PublicIdUtil.newAccountId());
        user.setEmail(EmailUtil.normalize(claims.email()));
        user.setFirstName(claims.firstName());
        user.setLastName(claims.lastName());
        user.setOnboardingStatus(OnboardingStatus.PROFILE_REQUIRED);

        User saved = userRepository.saveAndFlush(user);
        auditService.record(AuditAction.USER_PROVISIONED, saved.getId(), null, saved.getAccountId());
        return saved.getId();
    }
}
