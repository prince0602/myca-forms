package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.dto.response.AcceptInvitationResponse;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.exception.BusinessException;
import com.mynx.myca500.patient_forms.exception.ErrorCode;
import com.mynx.myca500.patient_forms.helper.PublicRateLimiter;
import com.mynx.myca500.patient_forms.service.CurrentUserService;
import com.mynx.myca500.patient_forms.service.InvitationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class PublicInvitationController {

    private final InvitationService invitationService;
    private final CurrentUserService currentUserService;
    private final PublicRateLimiter limiter;

    @GetMapping("/{token}")
    public ResponseEntity<?> preview(@PathVariable String token, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (!limiter.allowRequest(ip) || limiter.tooManyFailures(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, "60")
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("code", "RATE_LIMITED", "message", "Too many requests, try again later"));
        }
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invitationService.preview(token));
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.INVITATION_NOT_FOUND) limiter.recordFailure(ip); // slows token guessing
            throw e;
        }
    }

    @PostMapping("/{token}/accept")
    public AcceptInvitationResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable String token) {
        User user = currentUserService.resolve(jwt);
        return invitationService.accept(user, token);
    }
}
