package com.mynx.myca500.patient_forms.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "Member not found."),
    CANNOT_REMOVE_OWNER(HttpStatus.CONFLICT, "The workspace owner cannot be removed."),
    ONBOARDING_ALREADY_COMPLETE(HttpStatus.CONFLICT, "Profile is already complete."),
    BAA_NOT_ACCEPTED(HttpStatus.UNPROCESSABLE_ENTITY, "The BAA must be accepted."),
    PROFESSION_OTHER_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "Describe your profession."),
    BAA_VERSION_STALE(HttpStatus.CONFLICT, "The BAA has been updated. Please review the latest version."),
    NOT_WORKSPACE_OWNER(HttpStatus.FORBIDDEN, "Only the workspace owner can do this."),
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Workspace not found."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found."),
    PATIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Patient not found."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Email address is not verified."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed."),
    MALFORMED_BODY(HttpStatus.BAD_REQUEST, "Request body is malformed or contains invalid values."),


    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "Resource already exists."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized."),
    RESOURCE_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create the resource."),
    IMAGE_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "Image upload failed."),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Upload exceeds the maximum allowed size (5 MB per image)."),
    EMAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "Could not send email");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}

