package com.mynx.myca500.patient_forms.exception;

import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


final class ApiErrorResponses {

    private ApiErrorResponses() {
    }

    static ResponseEntity<List<Map<String, Object>>> of(ErrorCode errorCode) {
        return of(errorCode, null);
    }

    static ResponseEntity<List<Map<String, Object>>> of(ErrorCode errorCode, Map<String, String> fieldErrors) {
        return of(errorCode, null, fieldErrors);
    }

    static ResponseEntity<List<Map<String, Object>>> of(ErrorCode errorCode, String detailedMessage,
                                                       Map<String, String> fieldErrors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", String.valueOf(errorCode.getStatus().value()));
        body.put("errorCode", errorCode.name());
        body.put("message", errorCode.getStatus().getReasonPhrase());
        body.put("detailedMessage", detailedMessage != null ? detailedMessage : errorCode.getMessage());
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            body.put("fieldErrors", fieldErrors);
        }
        return ResponseEntity.status(errorCode.getStatus()).body(List.of(body));
    }
}
