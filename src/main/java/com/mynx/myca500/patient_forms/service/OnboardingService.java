package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.request.CompleteProfileRequest;
import com.mynx.myca500.patient_forms.dto.response.MeResponse;
import com.mynx.myca500.patient_forms.entity.BaaDocument;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.entity.Workspace;
import com.mynx.myca500.patient_forms.enums.AccountType;
import com.mynx.myca500.patient_forms.enums.AuditAction;
import com.mynx.myca500.patient_forms.enums.OnboardingStatus;
import com.mynx.myca500.patient_forms.enums.Profession;
import com.mynx.myca500.patient_forms.exception.BusinessException;
import com.mynx.myca500.patient_forms.exception.ErrorCode;
import com.mynx.myca500.patient_forms.model.ClientInfo;
import com.mynx.myca500.patient_forms.repository.UserRepository;
import com.mynx.myca500.patient_forms.util.NameUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final String FALLBACK_WORKSPACE_NAME = "My workspace";

    private final UserRepository userRepository;
    private final BaaService baaService;
    private final WorkspaceService workspaceService;
    private final MeService meService;
    private final AuditService auditService;

    @Transactional
    public MeResponse completeProfile(UUID userId, CompleteProfileRequest request, ClientInfo client) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validate(user, request);
        BaaDocument baa = baaService.requireCurrent(request.baaVersion());
        String phoneE164 = request.phone();

        user.setProfession(request.profession());
        user.setProfessionOther(request.profession() == Profession.OTHER ? request.professionOther().trim() : null);
        user.setTeamSize(request.teamSize());
        user.setPhoneE164(phoneE164);

        boolean business = StringUtils.hasText(request.businessName());
        Workspace workspace = workspaceService.createPersonalWorkspace(user,
                workspaceName(user, request.businessName(), business),
                business ? AccountType.BUSINESS : AccountType.PERSONAL,
                baa, client);

        user.setPersonalWorkspaceId(workspace.getId());
        user.setDefaultWorkspaceId(workspace.getId());
        user.setOnboardingStatus(OnboardingStatus.COMPLETE);

        auditService.record(AuditAction.PROFILE_COMPLETED, user.getId(), workspace.getId(), user.getAccountId());
        return meService.getMe(user);
    }

    private void validate(User user, CompleteProfileRequest request) {
        if (user.getOnboardingStatus() == OnboardingStatus.COMPLETE) {
            throw new BusinessException(ErrorCode.ONBOARDING_ALREADY_COMPLETE);
        }
        if (!request.baaAccepted()) {
            throw new BusinessException(ErrorCode.BAA_NOT_ACCEPTED);
        }
        if (request.profession() == Profession.OTHER && !StringUtils.hasText(request.professionOther())) {
            throw new BusinessException(ErrorCode.PROFESSION_OTHER_REQUIRED);
        }
    }

    private String workspaceName(User user, String businessName, boolean business) {
        String name = business ? businessName.trim() : NameUtil.fullName(user.getFirstName(), user.getLastName());
        return name.isBlank() ? FALLBACK_WORKSPACE_NAME : name;
    }
}
