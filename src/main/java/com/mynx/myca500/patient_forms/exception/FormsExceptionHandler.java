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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.List;

@RestControllerAdvice(assignableTypes = FormTemplateController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class FormsExceptionHandler {

    private final ObjectMapper objectMapper;

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApi(ApiException ex) {
        return problem(ex.getStatus(), ex.getErrors().get(0), ex.getErrors());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        String message = "Malformed request body";
        if (ex.getCause() instanceof InvalidFormatException ife) {
            message = "Invalid value '" + ife.getValue();
            Class<?> target = ife.getTargetType();
            if (target != null && target.isEnum()) {
                message += ". Accepted values: " + Arrays.stream(target.getEnumConstants())
                        .map(e -> objectMapper.convertValue(e, String.class)).toList();
            }
        }
        return problem(HttpStatus.BAD_REQUEST, message, List.of(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleBeanValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        String msg = "This template has changed. Please reload it and re-apply your changes.";
        return problem(HttpStatus.CONFLICT, msg, List.of(msg));
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
}
