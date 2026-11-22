package com.mynx.myca500.patient_forms.exception;

import com.mynx.myca500.patient_forms.controller.EmailController;
import com.mynx.myca500.patient_forms.controller.FormTemplateController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {FormTemplateController.class, EmailController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FormsExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<List<Map<String, Object>>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ApiErrorResponses.of(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<List<Map<String, Object>>> handleDuplicateResource(DuplicateResourceException ex) {
        return ApiErrorResponses.of(ErrorCode.DUPLICATE_RESOURCE, ex.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<List<Map<String, Object>>> handleAccessDenied(AccessDeniedException ex) {
        return ApiErrorResponses.of(ErrorCode.ACCESS_DENIED, ex.getMessage(), null);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<List<Map<String, Object>>> handleUnauthorizedAccess(UnauthorizedAccessException ex) {
        return ApiErrorResponses.of(ErrorCode.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler(ResourceCreationException.class)
    public ResponseEntity<List<Map<String, Object>>> handleResourceCreation(ResourceCreationException ex) {
        return ApiErrorResponses.of(ErrorCode.RESOURCE_CREATION_FAILED, ex.getMessage(), null);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<List<Map<String, Object>>> handleValidation(ValidationException ex) {
        return ApiErrorResponses.of(ErrorCode.VALIDATION_FAILED, ex.getMessage(), null);
    }

    @ExceptionHandler(ImageUploadException.class)
    public ResponseEntity<List<Map<String, Object>>> handleImageUpload(ImageUploadException ex) {
        return ApiErrorResponses.of(ErrorCode.IMAGE_UPLOAD_FAILED, ex.getMessage(), null);
    }

    /** Thrown by services with its own status (404 not found, 409 name in use, 400 template errors). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<List<Map<String, Object>>> handleApiException(ApiException ex) {
        ErrorCode code = switch (ex.getStatus()) {
            case NOT_FOUND -> ErrorCode.RESOURCE_NOT_FOUND;
            case CONFLICT -> ErrorCode.DUPLICATE_RESOURCE;
            default -> ErrorCode.VALIDATION_FAILED; // ApiException.badRequest (template errors)
        };
        return ApiErrorResponses.of(code, ex.getMessage(), null);
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<List<Map<String, Object>>> handleMail(MailException ex) {
        // Never echo the mail exception text: it can contain recipient addresses.
        return ApiErrorResponses.of(ErrorCode.EMAIL_SEND_FAILED);
    }
}
