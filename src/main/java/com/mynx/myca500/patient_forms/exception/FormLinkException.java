package com.mynx.myca500.patient_forms.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** A form link that cannot be used. Carries a stable error code for the frontend. */
@Getter
public class FormLinkException extends RuntimeException {

    public enum Code {
        LINK_INVALID(HttpStatus.NOT_FOUND, "This link is not valid"),
        LINK_EXPIRED(HttpStatus.GONE, "This link has expired"),
        LINK_REVOKED(HttpStatus.GONE, "This link has been revoked"),
        ALREADY_SUBMITTED(HttpStatus.CONFLICT, "This form has already been submitted");

        private final HttpStatus status;
        private final String message;

        Code(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    private final Code code;

    public FormLinkException(Code code) {
        super(code.message);
        this.code = code;
    }

    public HttpStatus getStatus() {
        return code.status;
    }
}
