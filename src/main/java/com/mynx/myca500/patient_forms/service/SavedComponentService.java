package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.SavedComponentRequest;
import com.mynx.myca500.patient_forms.dto.SavedComponentResponse;
import com.mynx.myca500.patient_forms.entity.SavedComponent;
import com.mynx.myca500.patient_forms.exception.ApiException;
import com.mynx.myca500.patient_forms.repository.SavedComponentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedComponentService {

    private final SavedComponentRepository components;

    @Transactional
    public SavedComponentResponse create(SavedComponentRequest req, String userId) {
        if (components.existsByNameIgnoreCase(req.name())) {
            throw ApiException.conflict("A component with this name already exists");
        }

        SavedComponent c = new SavedComponent();
        c.setName(req.name());
        c.setContent(req.content());
        c.setFields(req.fields());
        c.setCreatedBy(userId);
        return toResponse(components.saveAndFlush(c));
    }

    @Transactional(readOnly = true)
    public List<SavedComponentResponse> list() {
        return components.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SavedComponentResponse get(UUID id) {
        return toResponse(find(id));
    }

    /** Hard delete is safe: templates get a copy of the fields when a component is added, not a reference. */
    @Transactional
    public void delete(UUID id) {
        components.delete(find(id));
    }

    private SavedComponent find(UUID id) {
        return components.findById(id)
                .orElseThrow(() -> ApiException.notFound("Component not found: " + id));
    }

    private SavedComponentResponse toResponse(SavedComponent c) {
        return new SavedComponentResponse(c.getId().toString(), c.getName(), c.getContent(),
                c.getFields(), c.getCreatedBy(), c.getCreatedAt());
    }
}
