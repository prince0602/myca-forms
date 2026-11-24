package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.FormLinkResponse;
import com.mynx.myca500.patient_forms.dto.request.RecipientDetails;
import com.mynx.myca500.patient_forms.dto.request.SendFormToRecipientRequest;
import com.mynx.myca500.patient_forms.dto.response.RecipientResponse;
import com.mynx.myca500.patient_forms.dto.response.SendFormResponse;
import com.mynx.myca500.patient_forms.entity.Recipient;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.exception.BusinessException;
import com.mynx.myca500.patient_forms.exception.ErrorCode;
import com.mynx.myca500.patient_forms.model.WorkspaceAccess;
import com.mynx.myca500.patient_forms.repository.RecipientRepository;
import com.mynx.myca500.patient_forms.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipientService {

    private final RecipientRepository recipientRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final FormAssignmentService formAssignmentService;

    /**
     * Saves (or reuses) the recipient, then creates the form link pinned to the template's current
     * version and emails it to the recipient. The recipient is kept even if the email fails, so a
     * retry reuses it; the sent form itself is rolled back by createLink on email failure.
     */
    public SendFormResponse sendForm(User user, String publicWorkspaceId, SendFormToRecipientRequest request,
                                     String sentBy) {
        WorkspaceAccess access = workspaceAccessService.requireMember(user.getId(), publicWorkspaceId);
        Recipient recipient = upsert(access.workspace().getId(), user.getId(), request.recipient());
        FormLinkResponse link = formAssignmentService.createLink(request.templateId(), recipient.getId().toString(),
                recipient.getEmail(), request.expiresInHours(), sentBy);
        return new SendFormResponse(link.sentFormId(), recipient.getId().toString(), link.templateVersion(),
                link.expiresAt(), link.emailMessageId());
    }

    @Transactional(readOnly = true)
    public RecipientResponse get(User user, String publicWorkspaceId, String recipientId) {
        WorkspaceAccess access = workspaceAccessService.requireMember(user.getId(), publicWorkspaceId);
        Recipient recipient = parseId(recipientId)
                .flatMap(id -> recipientRepository.findByIdAndWorkspaceId(id, access.workspace().getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPIENT_NOT_FOUND));
        return RecipientResponse.from(recipient, publicWorkspaceId);
    }

    /**
     * Same workspace + email + first/last name (case-insensitive) = same recipient: it is reused and any
     * optional field provided now (phone, address, dob) replaces the stored value. Otherwise a new one is created.
     */
    Recipient upsert(UUID workspaceId, UUID userId, RecipientDetails details) {
        String email = EmailUtil.normalize(details.email());
        String firstName = details.firstName().trim();
        String lastName = details.lastName().trim();

        Optional<Recipient> existing = recipientRepository.findIdentity(workspaceId, email, firstName, lastName);
        if (existing.isPresent()) {
            Recipient r = existing.get();
            return applyOptionalFields(r, details) ? recipientRepository.save(r) : r;
        }

        Recipient r = new Recipient();
        r.setWorkspaceId(workspaceId);
        r.setFirstName(firstName);
        r.setLastName(lastName);
        r.setEmail(email);
        applyOptionalFields(r, details);
        r.setCreatedBy(userId);
        try {
            return recipientRepository.saveAndFlush(r);
        } catch (DataIntegrityViolationException e) {
            // The same recipient was created concurrently (ux_recipients_identity): reuse it.
            return recipientRepository.findIdentity(workspaceId, email, firstName, lastName).orElseThrow(() -> e);
        }
    }

    /** Copies the optional fields that were provided. Returns true if anything changed. */
    private static boolean applyOptionalFields(Recipient r, RecipientDetails d) {
        boolean changed = false;
        if (d.phoneNumber() != null && !d.phoneNumber().equals(r.getPhoneNumber())) {
            r.setPhoneNumber(d.phoneNumber());
            changed = true;
        }
        if (d.address() != null && !d.address().equals(r.getAddress())) {
            r.setAddress(d.address());
            changed = true;
        }
        if (d.dob() != null && !Objects.equals(d.dob(), r.getDob())) {
            r.setDob(d.dob());
            changed = true;
        }
        return changed;
    }

    static Optional<UUID> parseId(String id) {
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
