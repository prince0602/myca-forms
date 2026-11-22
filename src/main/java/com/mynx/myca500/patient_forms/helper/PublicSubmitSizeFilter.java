package com.mynx.myca500.patient_forms.helper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class PublicSubmitSizeFilter extends OncePerRequestFilter {

    private static final String SUBMIT_PATH = "/api/public/forms/submit";
    private static final String DRAFT_PATH = "/api/public/forms/draft";

    private final long maxBytes;

    public PublicSubmitSizeFilter(@Value("${app.public.submit.max-body-bytes:262144}") long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        boolean submit = "POST".equals(request.getMethod()) && SUBMIT_PATH.equals(path);
        boolean draft = "PUT".equals(request.getMethod()) && DRAFT_PATH.equals(path);
        return !submit && !draft;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long length = request.getContentLengthLong();
        if (length < 0) {
            reject(response, HttpStatus.LENGTH_REQUIRED, "LENGTH_REQUIRED");
            return;
        }
        if (length > maxBytes) {
            reject(response, HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE");
            return;
        }
        chain.doFilter(request, response);
    }

    private static void reject(HttpServletResponse response, HttpStatus status, String code) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + status.getReasonPhrase() + "\"}");
    }
}
