package com.mynx.myca500.patient_forms.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.mynx.myca500.patient_forms.controller.FormTemplateController;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(assignableTypes = FormTemplateController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class FormsExceptionHandler {

    private final ObjectMapper objectMapper;

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<List<Map<String, String>>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return new ResponseEntity<>(formatExceptionMsg(getErrorsMap(ex, HttpStatus.NOT_FOUND)), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<List<Map<String, String>>> handleDuplicateResourceException(
            DuplicateResourceException ex) {
        return new ResponseEntity<>(formatExceptionMsg(getErrorsMap(ex, HttpStatus.CONFLICT)), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<List<Map<String, String>>> handleAccessDeniedException(AccessDeniedException ex) {
        return new ResponseEntity<>(formatExceptionMsg(getErrorsMap(ex, HttpStatus.FORBIDDEN)), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<List<Map<String, String>>> handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
        return new ResponseEntity<>(formatExceptionMsg(getErrorsMap(ex, HttpStatus.UNAUTHORIZED)),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ResourceCreationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<List<Map<String, String>>> handleResourceCreationException(ResourceCreationException ex) {
        return new ResponseEntity<>(formatExceptionMsg(getErrorsMap(ex,
                HttpStatus.INTERNAL_SERVER_ERROR)),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<List<Map<String, String>>> handleValidationException(ValidationException ex) {
        return new ResponseEntity<>(formatExceptionMsg(getErrorsMap(ex,
                HttpStatus.BAD_REQUEST)),
                HttpStatus.BAD_REQUEST);
    }

    private static String jsonPath(InvalidFormatException ex) {
        StringBuilder sb = new StringBuilder();
        for (JsonMappingException.Reference ref : ex.getPath()) {
            if (ref.getFieldName() != null) sb.append(sb.length() > 0 ? "." : "").append(ref.getFieldName());
            else sb.append("[").append(ref.getIndex()).append("]");
        }
        return sb.toString();
    }

    private static ProblemDetail problem(HttpStatus status, String detail, List<String> errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("errors", errors);
        return pd;
    }

    private List<Map<String, String>> formatExceptionMsg(HashMap<String, String> errors) {
        List<Map<String, String>> errorList = new ArrayList<>();
        errorList.add(errors);
        return errorList;
    }

    private HashMap<String, String> getErrorsMap(RuntimeException ex, HttpStatus status) {
        return new HashMap<String, String>() {
            {
                put("code", String.valueOf(status.value()));
                put("detailedMessage", ex.getMessage());
                put("message", status.getReasonPhrase());
            }
        };
    }
}
