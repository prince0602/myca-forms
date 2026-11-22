package com.mynx.myca500.patient_forms.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FormLinkException extends RuntimeException {

    public enum Code {
        TOKEN_MISSING(HttpStatus.BAD_REQUEST,"Token missing"),
        LINK_INVALID(HttpStatus.NOT_FOUND, "This link is not valid"),
        LINK_EXPIRED(HttpStatus.GONE, "This link has expired"),
        LINK_REVOKED(HttpStatus.GONE, "This link has been revoked"),
        ALREADY_SUBMITTED(HttpStatus.CONFLICT, "This form has already been submitted"),
        STALE_VERSION(HttpStatus.CONFLICT, "This form was changed in another window, reload and try again");

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
