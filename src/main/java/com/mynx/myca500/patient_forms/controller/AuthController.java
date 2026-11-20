package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping("/me")
    public UserInfo me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        User user = userService.registerIfAbsent(jwt);

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
        return new UserInfo(
                jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name"),
                authorities,
                UserService.formatAccountId(user.getAccountId()));
    }



    public record UserInfo(
            String subject,
            String username,
            String email,
            String name,
            List<String> roles,
            String accountId) {}
}
