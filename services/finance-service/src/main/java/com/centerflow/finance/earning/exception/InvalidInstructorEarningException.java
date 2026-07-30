package com.centerflow.finance.earning.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidInstructorEarningException
        extends RuntimeException {

    public InvalidInstructorEarningException(
            String message
    ) {
        super(message);
    }
}