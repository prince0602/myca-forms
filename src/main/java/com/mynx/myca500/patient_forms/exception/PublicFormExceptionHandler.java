package com.mynx.myca500.patient_forms.exception;

import com.mynx.myca500.patient_forms.controller.PublicFormController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@RestControllerAdvice(assignableTypes = PublicFormController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublicFormExceptionHandler {

    @ExceptionHandler(FormSubmissionException.class)
    public ResponseEntity<List<Map<String, Object>>> invalid(FormSubmissionException ex) {
        Map<String, Object> body = body(HttpStatus.UNPROCESSABLE_ENTITY, "SUBMISSION_INVALID", ex.getMessage());
        body.put("fieldErrors", ex.getErrors());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(List.of(body));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<List<Map<String, Object>>> malformed(Exception ex) {
        return ResponseEntity.badRequest()
                .body(List.of(body(HttpStatus.BAD_REQUEST, "MALFORMED_BODY", "Request body is malformed")));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<List<Map<String, Object>>> validation(ValidationException ex) {
        return ResponseEntity.badRequest()
                .body(List.of(body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", ex.getMessage())));
    }

    private static Map<String, Object> body(HttpStatus status, String errorCode, String detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", String.valueOf(status.value()));
        m.put("errorCode", errorCode);
        m.put("message", status.getReasonPhrase());
        m.put("detailedMessage", detail);
        return m;
    }
}