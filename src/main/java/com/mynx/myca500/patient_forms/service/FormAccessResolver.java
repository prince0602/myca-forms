package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.entity.FormLink;
import com.mynx.myca500.patient_forms.entity.SentForm;
import com.mynx.myca500.patient_forms.exception.FormLinkException;
import com.mynx.myca500.patient_forms.exception.FormLinkException.Code;
import com.mynx.myca500.patient_forms.repository.SentFormRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class FormAccessResolver {

    private static final int MAX_TOKEN_LENGTH = 200;

    private final FormLinkService linkService;
    private final SentFormRepository sentForms;

    public record FormAccess(FormLink link, SentForm sentForm) {
        public boolean submitted() {
            return sentForm.getSubmittedAt() != null || link.getUsedAt() != null;
        }
    }


    public FormAccess forRead(String token) {
        return resolve(token, true);
    }

    public FormAccess forWrite(String token) {
        FormAccess access = resolve(token, false);
        if (access.submitted()) throw new FormLinkException(Code.ALREADY_SUBMITTED);
        return access;
    }

    private FormAccess resolve(String token, boolean touch) {
        if (token == null || token.isBlank()) throw new FormLinkException(Code.TOKEN_MISSING);
        String trimmed = token.trim();
        if (trimmed.length() > MAX_TOKEN_LENGTH) throw new FormLinkException(Code.LINK_INVALID);

        FormLink link = touch ? linkService.validate(trimmed) : linkService.findUsable(trimmed);
        SentForm form = sentForms.findById(link.getSentFormId())
                .orElseThrow(() -> new FormLinkException(Code.LINK_INVALID));

        if (form.getRevokedAt() != null) throw new FormLinkException(Code.LINK_REVOKED);
        if (form.getExpiresAt() != null && !Instant.now().isBefore(form.getExpiresAt())) {
            throw new FormLinkException(Code.LINK_EXPIRED);
        }
        return new FormAccess(link, form);
    }
}
