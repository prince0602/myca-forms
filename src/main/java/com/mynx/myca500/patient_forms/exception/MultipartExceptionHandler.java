package com.mynx.myca500.patient_forms.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Map;


@RestControllerAdvice
public class MultipartExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<List<Map<String, Object>>> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ApiErrorResponses.of(ErrorCode.PAYLOAD_TOO_LARGE);
    }
}
