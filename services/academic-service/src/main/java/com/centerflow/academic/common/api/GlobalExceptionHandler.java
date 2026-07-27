package com.centerflow.academic.common.api;

import com.centerflow.academic.common.exception.BatchCapacityExceededException;
import com.centerflow.academic.common.exception.BatchNotOpenForEnrollmentException;
import com.centerflow.academic.common.exception.BatchSessionNotFoundException;
import com.centerflow.academic.common.exception.InvalidSessionConfigurationException;
import com.centerflow.academic.common.exception.InvalidSessionStatusTransitionException;
import com.centerflow.academic.common.exception.SessionConfigurationLockedException;
import com.centerflow.academic.common.exception.SessionConflictException;
import com.centerflow.academic.common.exception.AcademicResourceUnavailableException;
import com.centerflow.academic.common.exception.BatchConfigurationLockedException;
import com.centerflow.academic.common.exception.BatchNotFoundException;
import com.centerflow.academic.common.exception.BatchScheduleNotFoundException;
import com.centerflow.academic.common.exception.BranchNotFoundException;
import com.centerflow.academic.common.exception.ClassroomNotFoundException;
import com.centerflow.academic.common.exception.CourseLevelNotFoundException;
import com.centerflow.academic.common.exception.CourseNotFoundException;
import com.centerflow.academic.common.exception.DuplicateBatchCodeException;
import com.centerflow.academic.common.exception.DuplicateBranchCodeException;
import com.centerflow.academic.common.exception.DuplicateClassroomCodeException;
import com.centerflow.academic.common.exception.DuplicateCourseCodeException;
import com.centerflow.academic.common.exception.DuplicateCourseLevelException;
import com.centerflow.academic.common.exception.DuplicateInstructorException;
import com.centerflow.academic.common.exception.DuplicateSeatReservationException;
import com.centerflow.academic.common.exception.InvalidBatchConfigurationException;
import com.centerflow.academic.common.exception.InvalidBatchStatusTransitionException;
import com.centerflow.academic.common.exception.InvalidScheduleConfigurationException;
import com.centerflow.academic.common.exception.InactiveBranchException;
import com.centerflow.academic.common.exception.InactiveCourseException;
import com.centerflow.academic.common.exception.InstructorNotFoundException;
import com.centerflow.academic.common.exception.InvalidCapacityRangeException;
import com.centerflow.academic.common.exception.InvalidPaginationException;
import com.centerflow.academic.common.exception.ScheduleConflictException;
import com.centerflow.academic.common.exception.SeatReservationNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            DuplicateBranchCodeException.class,
            DuplicateClassroomCodeException.class,
            DuplicateCourseCodeException.class,
            DuplicateCourseLevelException.class,
            DuplicateInstructorException.class,
            InactiveBranchException.class,
            InactiveCourseException.class,
            DuplicateBatchCodeException.class,
            AcademicResourceUnavailableException.class,
            InvalidBatchStatusTransitionException.class,
            BatchConfigurationLockedException.class,
            ScheduleConflictException.class,
            SessionConflictException.class,
            InvalidSessionStatusTransitionException.class,
            SessionConfigurationLockedException.class,
            BatchCapacityExceededException.class,
            BatchNotOpenForEnrollmentException.class,
            DuplicateSeatReservationException.class
    })
    public ResponseEntity<ApiErrorResponse>
    handleConflict(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler({
            BranchNotFoundException.class,
            ClassroomNotFoundException.class,
            CourseNotFoundException.class,
            CourseLevelNotFoundException.class,
            InstructorNotFoundException.class,
            BatchNotFoundException.class,
            BatchScheduleNotFoundException.class,
            BatchSessionNotFoundException.class,
            SeatReservationNotFoundException.class
    })
    public ResponseEntity<ApiErrorResponse>
    handleNotFound(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler({
            InvalidPaginationException.class,
            InvalidCapacityRangeException.class,
            InvalidBatchConfigurationException.class,
            InvalidScheduleConfigurationException.class,
            InvalidSessionConfigurationException.class
    })
    public ResponseEntity<ApiErrorResponse>
    handleInvalidRequest(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(fieldError ->
                        validationErrors.putIfAbsent(
                                fieldError.getField(),
                                fieldError.getDefaultMessage() == null
                                        ? "Invalid value"
                                        : fieldError.getDefaultMessage()
                        )
                );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                validationErrors
        );
    }

    private ResponseEntity<ApiErrorResponse>
    buildResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
        ApiErrorResponse response =
                new ApiErrorResponse(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        path,
                        validationErrors
                );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}