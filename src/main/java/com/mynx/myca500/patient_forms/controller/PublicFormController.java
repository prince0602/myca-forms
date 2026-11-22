package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.dto.SaveDraftRequest;
import com.mynx.myca500.patient_forms.dto.SubmitFormRequest;
import com.mynx.myca500.patient_forms.exception.FormLinkException;
import com.mynx.myca500.patient_forms.helper.PublicRateLimiter;
import com.mynx.myca500.patient_forms.service.PublicFormService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;
import java.util.function.Supplier;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/public/forms")
@RequiredArgsConstructor
public class PublicFormController {

    private static final String TOKEN_HEADER = "X-Form-Token";

    private final PublicFormService service;
    private final PublicRateLimiter limiter;

    @GetMapping
    public ResponseEntity<?> open(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (!limiter.allowRequest(ip) || limiter.tooManyFailures(ip)) return rateLimited();
        return guarded(ip, () -> service.open(token));
    }

    @PostMapping(path = "/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> submit(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @Valid @RequestBody SubmitFormRequest body,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (!limiter.allowSubmit(ip) || limiter.tooManyFailures(ip)) return rateLimited();
        return guarded(ip, () -> service.submit(token, body));
    }

    /** Autosave: replaces the draft answers. Uses the general request budget (clients should debounce). */
    @PutMapping(path = "/draft", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveDraft(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @Valid @RequestBody SaveDraftRequest body,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (!limiter.allowRequest(ip) || limiter.tooManyFailures(ip)) return rateLimited();
        return guarded(ip, () -> service.saveDraft(token, body));
    }

    @PostMapping("/uploads")
    public ResponseEntity<?> upload(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @RequestParam("ext") String extension,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (!limiter.allowSubmit(ip) || limiter.tooManyFailures(ip)) return rateLimited();
        return guarded(ip, () -> service.createUpload(token, extension));
    }

    private ResponseEntity<?> guarded(String ip, Supplier<?> call) {
        try {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .header("Pragma", "no-cache")
                    .body(call.get());
        } catch (FormLinkException e) {
            if (e.getCode() != FormLinkException.Code.STALE_VERSION) limiter.recordFailure(ip);
            throw e;
        }
    }

    private static ResponseEntity<?> rateLimited() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, "60")
                .cacheControl(CacheControl.noStore())
                .body(Map.of("code", "RATE_LIMITED", "message", "Too many requests, try again later"));
    }
}
