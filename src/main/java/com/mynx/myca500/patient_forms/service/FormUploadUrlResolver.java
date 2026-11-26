package com.mynx.myca500.patient_forms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class FormUploadUrlResolver {

    static final String UPLOAD_ROOT = "public-uploads/";

    private final S3PresignedUrlService presigner;

    static String uploadPrefix(UUID sentFormId) {
        return UPLOAD_ROOT + sentFormId + "/";
    }


    public Map<String, String> resolve(UUID sentFormId, Map<String, Object> answers) {
        if (answers == null || answers.isEmpty()) return Map.of();
        String prefix = uploadPrefix(sentFormId);

        Map<String, String> urls = new LinkedHashMap<>();
        for (Object value : answers.values()) {
            if (value instanceof String key && isOwnUpload(key, prefix) && !urls.containsKey(key)) {
                int slash = key.lastIndexOf('/');
                urls.put(key, presigner.createPreSignedGetUrl(key.substring(0, slash), key.substring(slash + 1)));
            }
        }
        return urls;
    }

    private static boolean isOwnUpload(String key, String prefix) {
        return key.startsWith(prefix)
                && key.length() > prefix.length()
                && key.indexOf('/', prefix.length()) < 0
                && !key.contains("..");
    }
}
