package com.mynx.myca500.patient_forms.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;


@RestControllerAdvice
public class BusinessExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<List<Map<String, Object>>> handle(BusinessException ex) {
        return ApiErrorResponses.of(ex.getErrorCode());
    }
}
