package com.centerflow.academic.common.exception;

import java.util.UUID;

public class AttendanceStudentMismatchException
        extends RuntimeException {

    public AttendanceStudentMismatchException(
            UUID enrollmentId
    ) {
        super(
                "Enrollment "
                        + enrollmentId
                        + " is already linked to a different student in this session"
        );
    }
}