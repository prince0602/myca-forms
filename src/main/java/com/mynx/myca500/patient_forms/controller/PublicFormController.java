package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.exception.FormLinkException;
import com.mynx.myca500.patient_forms.helper.PublicRateLimiter;
import com.mynx.myca500.patient_forms.service.PublicFormService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/public/forms")
@RequiredArgsConstructor
public class PublicFormController {

    private final PublicFormService service;
    private final PublicRateLimiter limiter;

    @GetMapping
    public ResponseEntity<?> open(
            @RequestHeader(value = "X-Form-Token", required = false) String token,
            HttpServletRequest request) {

        String ip = request.getRemoteAddr();
        if (!limiter.allowRequest(ip) || limiter.tooManyFailures(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, "60")
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("code", "RATE_LIMITED", "message", "Too many requests, try again later"));
        }

        try {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .header("Pragma", "no-cache")
                    .body(service.open(token));
        } catch (FormLinkException e) {
            limiter.recordFailure(ip);
            throw e;
        }
    }
}
