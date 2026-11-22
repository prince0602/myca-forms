package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.request.OnboardPatientRequest;
import com.mynx.myca500.patient_forms.dto.response.PatientResponse;
import com.mynx.myca500.patient_forms.entity.Patient;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.exception.BusinessException;
import com.mynx.myca500.patient_forms.exception.ErrorCode;
import com.mynx.myca500.patient_forms.model.WorkspaceAccess;
import com.mynx.myca500.patient_forms.repository.PatientRepository;
import com.mynx.myca500.patient_forms.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final WorkspaceAccessService workspaceAccessService;

    @Transactional
    public PatientResponse onboard(User user, String publicWorkspaceId, OnboardPatientRequest request) {
        WorkspaceAccess access = workspaceAccessService.requireMember(user.getId(), publicWorkspaceId);

        Patient patient = new Patient();
        patient.setWorkspaceId(access.workspace().getId());
        patient.setFullName(request.fullName().trim());
        patient.setAddress(request.address().trim());
        patient.setEmail(EmailUtil.normalize(request.email()));
        patient.setPhoneNumber(request.phoneNumber().trim());
        patient.setDob(request.dob());
        patient.setCreatedBy(user.getId());
        patientRepository.saveAndFlush(patient);

        return PatientResponse.from(patient, publicWorkspaceId);
    }

    @Transactional(readOnly = true)
    public PatientResponse get(User user, String publicWorkspaceId, String patientId) {
        WorkspaceAccess access = workspaceAccessService.requireMember(user.getId(), publicWorkspaceId);
        Patient patient = parseId(patientId)
                .flatMap(id -> patientRepository.findByIdAndWorkspaceId(id, access.workspace().getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PATIENT_NOT_FOUND));
        return PatientResponse.from(patient, publicWorkspaceId);
    }

    static Optional<UUID> parseId(String patientId) {
        try {
            return Optional.of(UUID.fromString(patientId));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
