package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.dto.request.OnboardPatientRequest;
import com.mynx.myca500.patient_forms.dto.response.PatientResponse;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.service.CurrentUserService;
import com.mynx.myca500.patient_forms.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/patients")
@RequiredArgsConstructor
public class PatientController {

    private final CurrentUserService currentUserService;
    private final PatientService patientService;

    @PostMapping
    public ResponseEntity<PatientResponse> onboard(@AuthenticationPrincipal Jwt jwt, @PathVariable String workspaceId,
                                                   @Valid @RequestBody OnboardPatientRequest request) {
        User user = currentUserService.resolve(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.onboard(user, workspaceId, request));
    }

    @GetMapping("/{patientId}")
    public PatientResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String workspaceId,
                               @PathVariable String patientId) {
        User user = currentUserService.resolve(jwt);
        return patientService.get(user, workspaceId, patientId);
    }
}
