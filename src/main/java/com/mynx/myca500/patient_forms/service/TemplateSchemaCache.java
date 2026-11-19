package com.mynx.myca500.patient_forms.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mynx.myca500.patient_forms.repository.FormTemplateVersionRepository;
import com.mynx.myca500.patient_forms.validation.CompiledFormSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class TemplateSchemaCache {

    private record Key(UUID templateId, int version) {}

    private final FormTemplateVersionRepository versions;

    private final Cache<Key, CompiledFormSchema> cache = Caffeine.newBuilder()
            .maximumSize(2_000)
            .expireAfterAccess(Duration.ofHours(12))
            .build();

    public CompiledFormSchema get(UUID templateId, int version) {
        return cache.get(new Key(templateId, version), k -> CompiledFormSchema.compile(
                versions.findByTemplateIdAndVersionNumber(k.templateId(), k.version())
                        .orElseThrow(() -> new IllegalStateException(
                                "Missing template " + k.templateId() + " v" + k.version()))
                        .getFields()));
    }
}
