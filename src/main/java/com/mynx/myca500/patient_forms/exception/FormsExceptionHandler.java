package com.mynx.myca500.patient_forms.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice("formsExceptionHandler")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FormsExceptionHandler {

    static final String RELOAD_MESSAGE = "This template has changed. Please reload it and re-apply your changes.";
    static final String NAME_IN_USE = "Name already in use by an active template";

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApi(ApiException ex) {
        return problem(ex.getStatus(), ex.getErrors().get(0), ex.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleBeanValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request body (check field types and enum values)", List.of());
    }

    /** Two saves raced at DB level. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, RELOAD_MESSAGE, List.of(RELOAD_MESSAGE));
    }

    /** Backstop for the partial unique index on active names. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        String root = String.valueOf(ex.getMostSpecificCause().getMessage());
        String msg = root.contains("uq_form_templates_active_name") ? NAME_IN_USE : "Request conflicts with existing data";
        return problem(HttpStatus.CONFLICT, msg, List.of(msg));
    }

    private static ProblemDetail problem(HttpStatus status, String detail, List<String> errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("errors", errors);
        return pd;
    }
}
