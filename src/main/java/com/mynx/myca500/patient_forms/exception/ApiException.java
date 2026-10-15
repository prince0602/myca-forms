package com.mynx.myca500.patient_forms.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> errors;

    private ApiException(HttpStatus status, List<String> errors) {
        super(String.join("; ", errors));
        this.status = status;
        this.errors = errors;
    }

    public static ApiException notFound(String message) { return new ApiException(HttpStatus.NOT_FOUND, List.of(message)); }

    public static ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT, List.of(message)); }

    public static ApiException badRequest(List<String> errors) { return new ApiException(HttpStatus.BAD_REQUEST, List.copyOf(errors)); }
}
