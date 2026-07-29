package com.centerflow.enrollment.api.error;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(

        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> validationErrors,
        String path

) {
}