package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.response.MeResponse;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.enums.OnboardingStatus;
import com.mynx.myca500.patient_forms.exception.BusinessException;
import com.mynx.myca500.patient_forms.exception.ErrorCode;
import com.mynx.myca500.patient_forms.mapper.UserMapper;
import com.mynx.myca500.patient_forms.model.WorkspaceAccess;
import com.mynx.myca500.patient_forms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeService {

    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;
    private final WorkspaceAccessService workspaceAccessService;

    @Transactional
    public MeResponse getMe(User user) {
        if (user.getOnboardingStatus() != OnboardingStatus.COMPLETE) {
            return UserMapper.toMe(user, List.of(), null);
        }
        return UserMapper.toMe(user,
                workspaceService.listAccessibleWorkspaces(user),
                workspaceService.findLandingWorkspaceId(user).orElse(null));
    }

    @Transactional
    public MeResponse switchWorkspace(UUID userId, String publicWorkspaceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        WorkspaceAccess access = workspaceAccessService.requireMember(userId, publicWorkspaceId);
        user.setDefaultWorkspaceId(access.workspace().getId());
        return getMe(user);
    }
}
