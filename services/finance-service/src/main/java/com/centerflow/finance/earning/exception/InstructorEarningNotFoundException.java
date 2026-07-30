package com.centerflow.finance.earning.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class InstructorEarningNotFoundException
        extends RuntimeException {

    public InstructorEarningNotFoundException(
            UUID earningId
    ) {
        super(
                "Instructor earning not found: "
                        + earningId
        );
    }
}