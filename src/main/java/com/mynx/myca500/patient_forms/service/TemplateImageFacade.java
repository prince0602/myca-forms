package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.FormFieldDto;
import com.mynx.myca500.patient_forms.dto.FormTemplateDetailResponse;
import com.mynx.myca500.patient_forms.dto.SaveFormTemplateRequest;
import com.mynx.myca500.patient_forms.exception.ValidationException;
import com.mynx.myca500.patient_forms.helper.FormFieldTraverser;
import com.mynx.myca500.patient_forms.validation.FormTemplateValidator;
import com.mynx.myca500.patient_forms.validation.S3Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.mynx.myca500.patient_forms.validation.S3Constants.FORMS_COMMON_FOLDER;

@Service
@RequiredArgsConstructor
public class TemplateImageFacade {

    private final FormTemplateService service;
    private final TemplateImageUploader uploader;
    private final ImageUrlResolver resolver;
    private final FormFieldTraverser traverser;
    private final FormTemplateValidator validator;

    private record Prepared(SaveFormTemplateRequest request, Collection<String> newFiles) {}

    public FormTemplateDetailResponse create(SaveFormTemplateRequest req,
                                             Map<String, MultipartFile> files, String userId) {
        Prepared p = prepare(req, files);
        FormTemplateDetailResponse saved;
        try {
            saved = service.create(p.request(), userId);
        } catch (RuntimeException ex) {
            uploader.deleteQuietly(p.newFiles());
            throw ex;
        }
        return render(saved);
    }

    public FormTemplateDetailResponse update(UUID id, SaveFormTemplateRequest req,
                                             Map<String, MultipartFile> files) {
        Prepared p = prepare(req, files);
        FormTemplateDetailResponse saved;
        try {
            saved = service.update(id, p.request());
        } catch (RuntimeException ex) {
            uploader.deleteQuietly(p.newFiles());
            throw ex;
        }
        return render(saved);
    }

    public FormTemplateDetailResponse get(UUID id)        { return render(service.get(id)); }


    /** Replaces stored image file names with presigned GET URLs so the client can render them. */
    public FormTemplateDetailResponse render(FormTemplateDetailResponse r) {
        return r.withFields(resolver.resolve(r.fields()));
    }


    private Prepared prepare(SaveFormTemplateRequest req, Map<String, MultipartFile> files) {
        List<FormFieldDto> fields = req.payload().fields();
        Set<String> refs = traverser.collectImages(fields);

        Set<String> unused = new HashSet<>(files.keySet());
        unused.removeAll(refs);
        if (!unused.isEmpty())
            throw new ValidationException("Uploaded files not referenced by any field: " + unused);

        Map<String, String> existing = new HashMap<>();
        for (String ref : refs) {
            if (!files.containsKey(ref)) existing.put(ref, normalizeExisting(ref));
        }

        // Reject an invalid template before spending S3 uploads on it.
        if (!files.isEmpty()) validator.validateAndNormalize(req.payload());

        Map<String, String> uploaded = files.isEmpty() ? Map.of() : uploader.uploadAll(files);
        try {
            SaveFormTemplateRequest resolved = req.withFields(traverser.mapImages(fields, r -> {
                String u = uploaded.get(r);
                return u != null ? u : existing.get(r);
            }));
            return new Prepared(resolved, uploaded.values());
        } catch (RuntimeException ex) {
            uploader.deleteQuietly(uploaded.values());
            throw ex;
        }
    }

    private String normalizeExisting(String ref) {
        String name = ref;
        if (ref.startsWith("https://") || ref.startsWith("http://")) {
            try {
                String path = URI.create(ref).getPath();
                String[] seg = path == null ? new String[0] : path.split("/");
                if (seg.length < 2 || !FORMS_COMMON_FOLDER.equals(seg[seg.length - 2]))
                    throw new ValidationException("Invalid image reference.");
                name = seg[seg.length - 1];
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid image reference.");
            }
        }
        if (!S3Constants.isSafeFileName(name)) throw new ValidationException("Invalid image reference.");
        return name;
    }
}