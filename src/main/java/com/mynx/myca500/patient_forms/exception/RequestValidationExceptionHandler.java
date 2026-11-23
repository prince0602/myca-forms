package com.mynx.myca500.patient_forms.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.mynx.myca500.patient_forms.controller.EmailController;
import com.mynx.myca500.patient_forms.controller.FormTemplateController;
import com.mynx.myca500.patient_forms.controller.InvitationController;
import com.mynx.myca500.patient_forms.controller.MeController;
import com.mynx.myca500.patient_forms.controller.MemberController;
import com.mynx.myca500.patient_forms.controller.OnboardingController;
import com.mynx.myca500.patient_forms.controller.PatientController;
import com.mynx.myca500.patient_forms.controller.WorkspaceController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {MeController.class, OnboardingController.class,
        WorkspaceController.class, MemberController.class,
        FormTemplateController.class, EmailController.class, PatientController.class,
        InvitationController.class})
public class RequestValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<Map<String, Object>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ApiErrorResponses.of(ErrorCode.VALIDATION_FAILED, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<List<Map<String, Object>>> handleUnreadable(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            return ApiErrorResponses.of(ErrorCode.MALFORMED_BODY, Map.of(jsonPath(mapping), "invalid value"));
        }
        return ApiErrorResponses.of(ErrorCode.MALFORMED_BODY);
    }

    private static String jsonPath(JsonMappingException ex) {
        StringBuilder sb = new StringBuilder();
        for (JsonMappingException.Reference ref : ex.getPath()) {
            if (ref.getFieldName() != null) {
                sb.append(sb.isEmpty() ? "" : ".").append(ref.getFieldName());
            } else {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.toString();
    }
}
