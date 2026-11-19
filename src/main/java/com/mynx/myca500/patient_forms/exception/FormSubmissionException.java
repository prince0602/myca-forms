package com.mynx.myca500.patient_forms.exception;

import com.mynx.myca500.patient_forms.dto.AnswerError;
import lombok.Getter;

import java.util.List;

@Getter
public class FormSubmissionException extends RuntimeException {

    private final transient List<AnswerError> errors;

    public FormSubmissionException(List<AnswerError> errors) {
        super(errors.size() + (errors.size() == 1 ? " field is invalid" : " fields are invalid"));
        this.errors = List.copyOf(errors);
    }
}
