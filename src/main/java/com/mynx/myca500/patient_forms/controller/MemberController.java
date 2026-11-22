package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.dto.response.MemberResponse;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.service.CurrentUserService;
import com.mynx.myca500.patient_forms.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/members")
@RequiredArgsConstructor
public class MemberController {

    private final CurrentUserService currentUserService;
    private final MemberService memberService;

    @GetMapping
    public List<MemberResponse> listMembers(@AuthenticationPrincipal Jwt jwt, @PathVariable String workspaceId) {
        User user = currentUserService.resolve(jwt);
        return memberService.listMembers(user, workspaceId);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal Jwt jwt, @PathVariable String workspaceId,
                                             @PathVariable String accountId) {
        User user = currentUserService.resolve(jwt);
        memberService.removeMember(user, workspaceId, accountId);
        return ResponseEntity.noContent().build();
    }
}

