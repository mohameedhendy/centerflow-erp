package com.centerflow.academic.common.exception;

public class InvalidAcademicReportPeriodException
        extends InvalidBatchConfigurationException {

    public InvalidAcademicReportPeriodException(
            String message
    ) {
        super(message);
    }
}