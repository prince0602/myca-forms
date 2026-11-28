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
    RECIPIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Recipient not found."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Email address is not verified."),
    PLAN_INVITES_NOT_ALLOWED(HttpStatus.FORBIDDEN, "Your plan does not allow inviting members."),
    CANNOT_INVITE_SELF(HttpStatus.UNPROCESSABLE_ENTITY, "You cannot invite yourself."),
    ALREADY_MEMBER(HttpStatus.CONFLICT, "This person is already a member of the workspace."),
    INVITATION_ALREADY_PENDING(HttpStatus.CONFLICT, "An invitation for this email is already pending."),
    INVITATION_RESEND_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "Too many invitations sent to this email. Try again later."),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Invitation not found."),
    INVITATION_EXPIRED(HttpStatus.GONE, "This invitation has expired. Ask the workspace owner to send a new one."),
    INVITATION_REVOKED(HttpStatus.GONE, "This invitation has been cancelled."),
    INVITATION_ALREADY_ACCEPTED(HttpStatus.CONFLICT, "This invitation has already been accepted."),
    INVITATION_EMAIL_MISMATCH(HttpStatus.FORBIDDEN, "This invitation was sent to a different email address."),
    ONBOARDING_REQUIRED(HttpStatus.CONFLICT, "Complete your profile before accepting the invitation."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed."),
    MALFORMED_BODY(HttpStatus.BAD_REQUEST, "Request body is malformed or contains invalid values."),


    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "Resource already exists."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized."),
    RESOURCE_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create the resource."),
    IMAGE_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "Image upload failed."),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Upload exceeds the maximum allowed size (5 MB per image)."),
    EMAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "Could not send email"),

    FORM_LIMIT_REACHED(HttpStatus.FORBIDDEN, "This workspace has used all of its forms for this month. Upgrade to send more."),
    PLAN_SEAT_LIMIT_REACHED(HttpStatus.FORBIDDEN, "This workspace has reached the maximum number of users for its plan."),
    PLAN_NOT_PURCHASABLE(HttpStatus.UNPROCESSABLE_ENTITY, "This plan cannot be purchased in the app."),
    ALREADY_ON_PLAN(HttpStatus.CONFLICT, "This workspace is already on this plan."),
    PAYMENT_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "Card payments are not available right now."),
    PAYMENT_VERIFICATION_FAILED(HttpStatus.PAYMENT_REQUIRED, "The card payment could not be verified."),
    PAYMENT_ALREADY_USED(HttpStatus.CONFLICT, "This payment has already been used.");

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

