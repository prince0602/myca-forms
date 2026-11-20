package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.dto.request.CompleteProfileRequest;
import com.mynx.myca500.patient_forms.dto.response.MeResponse;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.service.CurrentUserService;
import com.mynx.myca500.patient_forms.service.OnboardingService;
import com.mynx.myca500.patient_forms.util.ClientInfoUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final CurrentUserService currentUserService;
    private final OnboardingService onboardingService;

    @PutMapping("/profile")
    public MeResponse completeProfile(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody CompleteProfileRequest request,
                                      HttpServletRequest httpRequest) {
        User user = currentUserService.resolve(jwt);
        return onboardingService.completeProfile(user.getId(), request, ClientInfoUtil.from(httpRequest));
    }
}
