package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.commons.exceptions.ResourceCreationException;
import com.mynx.myca500.commons.exceptions.ValidationException;
import com.mynx.myca500.patient_forms.dto.FormTemplateDetailResponse;
import com.mynx.myca500.patient_forms.dto.FormTemplateListItem;
import com.mynx.myca500.patient_forms.dto.Paginated;
import com.mynx.myca500.patient_forms.dto.SaveFormTemplateRequest;
import com.mynx.myca500.patient_forms.service.FormTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/form-templates")
public class FormTemplateController {

    @Autowired
    private FormTemplateService service;

    @GetMapping
    public Paginated<FormTemplateListItem> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    public FormTemplateDetailResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<FormTemplateDetailResponse> create(@Valid @RequestBody SaveFormTemplateRequest request,
                                                             HttpServletRequest httpServletRequest) {
       String userId= getUserId(httpServletRequest);
        return ResponseEntity.ok(service.create(request,userId));
    }

    @PutMapping("/{id}")
    public FormTemplateDetailResponse update(@PathVariable UUID id, @Valid @RequestBody SaveFormTemplateRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/activate")
    public FormTemplateDetailResponse activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/deactivate")
    public FormTemplateDetailResponse deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<FormTemplateDetailResponse> duplicate(@PathVariable UUID id) {
        return ResponseEntity.ok(service.duplicate(id));
    }

    private String getUserId(HttpServletRequest request) {
        try {
            return request.getHeader("X-User-ID");
        } catch (ValidationException e) {
            throw new ValidationException(e.getMessage());
        } catch (ResourceCreationException e) {
            throw new ResourceCreationException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
