package com.centerflow.finance.earning.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InstructorEarningConflictException
        extends RuntimeException {

    public InstructorEarningConflictException(
            String message
    ) {
        super(message);
    }
}