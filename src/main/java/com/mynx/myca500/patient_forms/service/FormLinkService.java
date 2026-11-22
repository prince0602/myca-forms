package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.entity.FormLink;
import com.mynx.myca500.patient_forms.exception.FormLinkException;
import com.mynx.myca500.patient_forms.exception.FormLinkException.Code;
import com.mynx.myca500.patient_forms.repository.FormLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FormLinkService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final FormLinkRepository repo;

    public String create(UUID sentFormId, Instant expiresAt) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        FormLink link = new FormLink();
        link.setTokenHash(hash(token));
        link.setSentFormId(sentFormId);
        link.setExpiresAt(expiresAt);
        repo.save(link);
        return token;
    }

    public FormLink validate(String token) {
        if (token == null || token.isBlank()) throw new FormLinkException(Code.LINK_INVALID);

        FormLink link = repo.findByTokenHash(hash(token))
                .orElseThrow(() -> new FormLinkException(Code.LINK_INVALID));
        Instant now = Instant.now();
        if (link.getRevokedAt() != null) throw new FormLinkException(Code.LINK_REVOKED);

        if (link.getExpiresAt() != null && !now.isBefore(link.getExpiresAt())) {
            throw new FormLinkException(Code.LINK_EXPIRED);
        }

        link.setLastAccessedAt(now);
        return repo.save(link);
    }

    /** @throws FormLinkException ALREADY_SUBMITTED (409) if the form was submitted through this link */
    public void requireNotSubmitted(FormLink link) {
        if (link.getUsedAt() != null) throw new FormLinkException(Code.ALREADY_SUBMITTED);
    }

    /** Call only when the form is submitted; closes the link for further writes. */
    public void markUsed(FormLink link) {
        link.setUsedAt(Instant.now());
        repo.save(link);
    }

    private static String hash(String token) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
