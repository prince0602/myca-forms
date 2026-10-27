package com.mynx.myca500.patient_forms.service;


import com.mynx.myca500.patient_forms.dto.FormFieldDto;
import com.mynx.myca500.patient_forms.dto.FormTemplateDetailResponse;
import com.mynx.myca500.patient_forms.dto.FormTemplateListItem;
import com.mynx.myca500.patient_forms.dto.FormTemplatePayload;
import com.mynx.myca500.patient_forms.dto.Paginated;
import com.mynx.myca500.patient_forms.dto.SaveFormTemplateRequest;
import com.mynx.myca500.patient_forms.entity.FormTemplate;
import com.mynx.myca500.patient_forms.entity.FormTemplateVersion;
import com.mynx.myca500.patient_forms.enums.SaveMode;
import com.mynx.myca500.patient_forms.enums.TemplateStatus;
import com.mynx.myca500.patient_forms.exception.ApiException;
import com.mynx.myca500.patient_forms.repository.FormTemplateRepository;
import com.mynx.myca500.patient_forms.repository.FormTemplateVersionRepository;
import com.mynx.myca500.patient_forms.repository.SentFormRepository;
import com.mynx.myca500.patient_forms.validation.FormTemplateValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormTemplateService {

    static final String NAME_IN_USE = "Name already in use by an active template";
    static final String STALE = "This template has changed. Please reload it and re-apply your changes.";

    private final FormTemplateRepository templates;
    private final FormTemplateVersionRepository versions;
    private final SentFormRepository sentForms;
    private final FormTemplateValidator validator;
    private final FormTemplateMapper mapper;


    @Transactional(readOnly = true)
    public Paginated<FormTemplateListItem> list(int page, int size) {

        PageRequest pageable = PageRequest.of(
                Math.max(page - 1, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Page<FormTemplate> result = templates.findAll(pageable);

        List<FormTemplateListItem> items = result.getContent()
                .stream()
                .map(mapper::toListItem)
                .toList();

        return new Paginated<>(items, result.getTotalElements());

    }


    @Transactional(readOnly = true)
    public FormTemplateDetailResponse get(UUID id) {
        FormTemplate t = find(id);
        return mapper.toDetail(t, currentVersion(t));
    }


    @Transactional
    public FormTemplateDetailResponse create(SaveFormTemplateRequest req, String userId) {

        FormTemplatePayload p = req.payload();
        List<FormFieldDto> fields = validator.validateAndNormalize(p);
        boolean activate = req.mode() == SaveMode.SAVE_AND_ACTIVATE;
        if (activate && templates.existsByStatusAndNameIgnoreCase(TemplateStatus.ACTIVE, p.label())) {
            throw ApiException.conflict(NAME_IN_USE);
        }

        FormTemplate t = new FormTemplate();
        t.setName(p.label());
        t.setDescription(p.description());
        t.setStatus(activate ? TemplateStatus.ACTIVE : TemplateStatus.INACTIVE);
        t.setCreatedBy(userId);
        t.setUpdatedBy(userId);
        t.setCurrentVersion(1);
        templates.saveAndFlush(t);

        return mapper.toDetail(t, saveVersion(t, p, fields));

    }

    @Transactional
    public FormTemplateDetailResponse update(UUID id, SaveFormTemplateRequest req) {
        FormTemplate t = find(id);
        if (req.baseVersion() == null) {
            throw ApiException.badRequest(List.of("baseVersion is required when editing a template"));
        }
        if (req.baseVersion() != t.getCurrentVersion()) {
            throw ApiException.conflict(STALE);
        }

        FormTemplatePayload p = req.payload();
        List<FormFieldDto> fields = validator.validateAndNormalize(p);
        boolean activate = req.mode() == SaveMode.SAVE_AND_ACTIVATE;
        if ((activate || t.getStatus() == TemplateStatus.ACTIVE) && nameTakenByOther(p.label(), id)) {
            throw ApiException.conflict(NAME_IN_USE);
        }

        t.setName(p.label());
        t.setDescription(p.description());
        t.setCurrentVersion(t.getCurrentVersion() + 1);
        if (activate) {
            t.setStatus(TemplateStatus.ACTIVE);
        }
        templates.saveAndFlush(t);

        return mapper.toDetail(t, saveVersion(t, p, fields));

    }

    @Transactional
    public FormTemplateDetailResponse activate(UUID id) {

        FormTemplate t = find(id);
        if (t.getStatus() != TemplateStatus.ACTIVE) {
            if (nameTakenByOther(t.getName(), id)) {
                throw ApiException.conflict(NAME_IN_USE);
            }
            t.setStatus(TemplateStatus.ACTIVE);
            templates.saveAndFlush(t);
        }
        return mapper.toDetail(t, currentVersion(t));

    }

    @Transactional
    public FormTemplateDetailResponse deactivate(UUID id) {

        FormTemplate t = find(id);
        if (t.getStatus() != TemplateStatus.INACTIVE) {
            t.setStatus(TemplateStatus.INACTIVE);
            templates.saveAndFlush(t);
        }
        return mapper.toDetail(t, currentVersion(t));

    }

    @Transactional
    public FormTemplateDetailResponse duplicate(UUID id) {
        FormTemplate source = find(id);
        FormTemplateVersion sourceVersion = currentVersion(source);

        FormTemplate copy = new FormTemplate();
        copy.setName(source.getName() + " - copy");
        copy.setDescription(source.getDescription());
        copy.setStatus(TemplateStatus.INACTIVE);
        copy.setCurrentVersion(1);
        templates.saveAndFlush(copy);

        FormTemplateVersion v = new FormTemplateVersion();
        v.setTemplateId(copy.getId());
        v.setVersionNumber(1);
        v.setLabel(copy.getName());
        v.setDescription(sourceVersion.getDescription());
        v.setExpirationHours(sourceVersion.getExpirationHours());
        v.setFields(sourceVersion.getFields());
        return mapper.toDetail(copy, versions.save(v));

    }


    private FormTemplate find(UUID id) {
        return templates.findById(id).orElseThrow(() -> ApiException.notFound("Template not found: " + id));
    }

    private FormTemplateVersion currentVersion(FormTemplate t) {
        return versions.findByTemplateIdAndVersionNumber(t.getId(), t.getCurrentVersion())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing version " + t.getCurrentVersion() + " for template " + t.getId()));
    }

    private boolean nameTakenByOther(String name, UUID selfId) {
        return templates.existsByStatusAndNameIgnoreCaseAndIdNot(TemplateStatus.ACTIVE, name, selfId);
    }

    private FormTemplateVersion saveVersion(FormTemplate t, FormTemplatePayload p, List<FormFieldDto> fields) {
        FormTemplateVersion v = new FormTemplateVersion();
        v.setTemplateId(t.getId());
        v.setVersionNumber(t.getCurrentVersion());
        v.setLabel(p.label());
        v.setDescription(p.description());
        v.setExpirationHours(p.expirationHours());
        v.setFields(fields);
        return versions.save(v);
    }
}
