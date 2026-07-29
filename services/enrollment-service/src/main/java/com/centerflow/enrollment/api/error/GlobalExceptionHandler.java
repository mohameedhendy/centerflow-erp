package com.centerflow.enrollment.api.error;

import com.centerflow.enrollment.domain.InvalidEnrollmentStatusTransitionException;
import com.centerflow.enrollment.exception.EnrollmentConflictException;
import com.centerflow.enrollment.exception.EnrollmentNotFoundException;
import com.centerflow.enrollment.exception.InvalidPaginationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EnrollmentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse>
    handleEnrollmentNotFound(
            EnrollmentNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                Map.of(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(EnrollmentConflictException.class)
    public ResponseEntity<ApiErrorResponse>
    handleEnrollmentConflict(
            EnrollmentConflictException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                Map.of(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            InvalidEnrollmentStatusTransitionException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleInvalidStatusTransition(
            InvalidEnrollmentStatusTransitionException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                Map.of(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidPaginationException.class)
    public ResponseEntity<ApiErrorResponse>
    handleInvalidPagination(
            InvalidPaginationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                Map.of(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            MethodArgumentTypeMismatchException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: "
                        + exception.getName(),
                Map.of(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse>
    handleValidationFailure(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        for (
                FieldError fieldError
                : exception.getBindingResult().getFieldErrors()
        ) {
            validationErrors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                validationErrors,
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            Map<String, String> validationErrors,
            String path
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                validationErrors,
                path
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}