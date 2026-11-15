package com.mynx.myca500.patient_forms.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mynx.myca500.patient_forms.dto.FormFieldDto;
import com.mynx.myca500.patient_forms.helper.FormFieldTraverser;
import com.mynx.myca500.patient_forms.validation.S3Constants;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ImageUrlResolver {

    private final S3PresignedUrlService presignService;
    private final FormFieldTraverser traverser;
    private final Cache<String, String> cache;

    public ImageUrlResolver(S3PresignedUrlService presignService, FormFieldTraverser traverser) {
        this.presignService = presignService;
        this.traverser = traverser;
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(presignService.getExpiryMinutes()).dividedBy(2))
                .build();
    }

    public List<FormFieldDto> resolve(List<FormFieldDto> fields) {
        Set<String> names = traverser.collectImages(fields);
        if (names.isEmpty()) return fields;

        Map<String, String> urls = new HashMap<>(names.size());
        for (String name : names) {
            if (!S3Constants.isSafeFileName(name)) { urls.put(name, null); continue; }
            urls.put(name, cache.get(name,
                    n -> presignService.createPreSignedGetUrl(S3Constants.FORMS_COMMON_FOLDER, n)));
        }
        return traverser.mapImages(fields, n -> urls.get(n));
    }
}