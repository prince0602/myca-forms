package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.entity.FormLink;
import com.mynx.myca500.patient_forms.exception.ApiException;
import com.mynx.myca500.patient_forms.repository.FormLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FormLinkService {

    private final FormLinkRepository repo;

    public String create(Long formId, Long patientId, int hours) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        FormLink link = new FormLink();
        link.setTokenHash(hash(token));
        link.setFormId(formId);
        link.setPatientId(patientId);
        link.setExpiresAt(Instant.now().plus(hours, ChronoUnit.HOURS));
        repo.save(link);
        return token;
    }

    public FormLink validate(String token) {
        FormLink link = repo.findByTokenHash(hash(token))
                .orElseThrow(() -> ApiException.badRequest(List.of("Invalid link")));
        if (link.getUsedAt() != null) throw ApiException.badRequest(List.of("Link already used"));
        if (link.getExpiresAt().isBefore(Instant.now())) throw ApiException.badRequest(List.of("Link expired"));
        return link;
    }

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
