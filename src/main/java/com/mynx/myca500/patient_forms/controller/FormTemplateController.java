package com.mynx.myca500.patient_forms.controller;


import com.mynx.myca500.patient_forms.dto.FormLinkRequest;
import com.mynx.myca500.patient_forms.dto.FormLinkResponse;
import com.mynx.myca500.patient_forms.dto.FormTemplateDetailResponse;
import com.mynx.myca500.patient_forms.dto.FormTemplateListItem;
import com.mynx.myca500.patient_forms.dto.Paginated;
import com.mynx.myca500.patient_forms.dto.SaveFormTemplateRequest;
import com.mynx.myca500.patient_forms.dto.SavedComponentRequest;
import com.mynx.myca500.patient_forms.dto.SavedComponentResponse;
import com.mynx.myca500.patient_forms.exception.ResourceCreationException;
import com.mynx.myca500.patient_forms.exception.ValidationException;
import com.mynx.myca500.patient_forms.service.FormAssignmentService;
import com.mynx.myca500.patient_forms.service.FormTemplateService;
import com.mynx.myca500.patient_forms.service.SavedComponentService;
import com.mynx.myca500.patient_forms.service.TemplateImageFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/form-templates")
public class FormTemplateController {

    @Autowired
    private FormTemplateService service;

    @Autowired
    private TemplateImageFacade facade;


    @Autowired
    private SavedComponentService savedComponentService;

    @Autowired
    private FormAssignmentService formAssignmentService;

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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FormTemplateDetailResponse> create(
            @RequestPart("request") @Valid SaveFormTemplateRequest request,
            @RequestParam(required = false) MultiValueMap<String, MultipartFile> parts,
            HttpServletRequest http) {
        return ResponseEntity.ok(facade.create(request, toFileMap(parts), getUserId(http)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FormTemplateDetailResponse update(@PathVariable UUID id,
                                             @RequestPart("request") @Valid SaveFormTemplateRequest request,
                                             @RequestParam(required = false) MultiValueMap<String, MultipartFile> parts) {
        return facade.update(id, request, toFileMap(parts));
    }

    private Map<String, MultipartFile> toFileMap(MultiValueMap<String, MultipartFile> parts) {
        Map<String, MultipartFile> files = new LinkedHashMap<>();
        if (parts != null) parts.forEach((ref, list) -> {
            if (list.size() != 1) throw new ValidationException("Duplicate upload part: " + ref);
            files.put(ref, list.get(0));
        });
        return files;
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

    /** Creates a shareable fill link for a patient, pinned to the template's current version. */
    @PostMapping("/{id}/links")
    public ResponseEntity<FormLinkResponse> createLink(@PathVariable UUID id,
                                                       @Valid @RequestBody FormLinkRequest request,
                                                       @AuthenticationPrincipal Jwt jwt) {
        FormLinkResponse body = formAssignmentService.createLink(id, request.patientId(), request.email(),
                request.expiresInHours(), jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/component")
    public ResponseEntity<SavedComponentResponse> create(@Valid @RequestBody SavedComponentRequest request,
                                                         HttpServletRequest httpServletRequest) {
        String userId = getUserId(httpServletRequest);
        SavedComponentResponse body = savedComponentService.create(request, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(body.id()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping("/component")
    public List<SavedComponentResponse> list() {
        return savedComponentService.list();
    }

    @GetMapping("/component/{id}")
    public SavedComponentResponse getComponent(@PathVariable UUID id) {
        return savedComponentService.get(id);
    }

    @DeleteMapping("component/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        savedComponentService.delete(id);
        return ResponseEntity.noContent().build();
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
