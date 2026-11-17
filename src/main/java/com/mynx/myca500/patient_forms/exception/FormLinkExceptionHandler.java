package com.mynx.myca500.patient_forms.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/**
 * Global (not controller-scoped) so link errors map correctly from any controller,
 * including the upcoming public form endpoints. Body shape matches FormsExceptionHandler,
 * plus an "errorCode" the frontend can switch on.
 */
@RestControllerAdvice
public class FormLinkExceptionHandler {

    @ExceptionHandler(FormLinkException.class)
    public ResponseEntity<List<Map<String, String>>> handle(FormLinkException ex) {
        Map<String, String> body = Map.of(
                "code", String.valueOf(ex.getStatus().value()),
                "errorCode", ex.getCode().name(),
                "message", ex.getStatus().getReasonPhrase(),
                "detailedMessage", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(List.of(body));
    }
}
