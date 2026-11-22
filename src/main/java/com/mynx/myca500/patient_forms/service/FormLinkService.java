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
                FormLink link = findUsable(token);
                link.setLastAccessedAt(Instant.now());
                return repo.save(link);
           }



    public FormLink findUsable(String token) {
        if (token == null || token.isBlank()) throw new FormLinkException(Code.LINK_INVALID);

        FormLink link = repo.findByTokenHash(hash(token))
                .orElseThrow(() -> new FormLinkException(Code.LINK_INVALID));

        if (link.getExpiresAt() != null && !Instant.now().isBefore(link.getExpiresAt())) {
            throw new FormLinkException(Code.LINK_EXPIRED);
        }

        return link;
    }

    public boolean markUsed(UUID linkId, Instant now) {
                return repo.markUsed(linkId, now) == 1;
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
