package com.centerflow.academic.common.exception;

public class DuplicateInstructorException
        extends RuntimeException {

    private DuplicateInstructorException(
            String message
    ) {
        super(message);
    }

    private DuplicateInstructorException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }

    public static DuplicateInstructorException
    forCode(String code) {
        return new DuplicateInstructorException(
                "An instructor already exists with code: "
                        + code
        );
    }

    public static DuplicateInstructorException
    forEmail(String email) {
        return new DuplicateInstructorException(
                "An instructor already exists with email: "
                        + email
        );
    }

    public static DuplicateInstructorException
    forConflictingData(
            String code,
            String email,
            Throwable cause
    ) {
        return new DuplicateInstructorException(
                "An instructor already exists with code "
                        + code
                        + " or email "
                        + email,
                cause
        );
    }

    public static DuplicateInstructorException
    forEmail(
            String email,
            Throwable cause
    ) {
        return new DuplicateInstructorException(
                "An instructor already exists with email: "
                        + email,
                cause
        );
    }
}