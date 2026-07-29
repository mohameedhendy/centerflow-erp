package com.centerflow.enrollment.application;

import com.centerflow.enrollment.api.dto.CreateEnrollmentRequest;
import com.centerflow.enrollment.api.dto.EnrollmentResponse;
import com.centerflow.enrollment.domain.Enrollment;
import com.centerflow.enrollment.domain.EnrollmentStatus;
import com.centerflow.enrollment.exception.EnrollmentConflictException;
import com.centerflow.enrollment.exception.EnrollmentNotFoundException;
import com.centerflow.enrollment.number.EnrollmentNumberGenerator;
import com.centerflow.enrollment.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class EnrollmentService {

    private static final Set<EnrollmentStatus>
            OPEN_ENROLLMENT_STATUSES = EnumSet.of(
            EnrollmentStatus.PENDING_PAYMENT,
            EnrollmentStatus.ACTIVE,
            EnrollmentStatus.SUSPENDED
    );

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentNumberGenerator numberGenerator;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            EnrollmentNumberGenerator numberGenerator
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.numberGenerator = numberGenerator;
    }

    @Transactional
    public EnrollmentResponse createEnrollment(
            CreateEnrollmentRequest request
    ) {
        validateNoOpenEnrollment(
                request.studentId(),
                request.batchId()
        );

        String enrollmentNumber =
                numberGenerator.nextNumber();

        Enrollment enrollment = Enrollment.create(
                enrollmentNumber,
                request.studentId(),
                request.batchId()
        );

        Enrollment savedEnrollment =
                enrollmentRepository.save(enrollment);

        return EnrollmentResponse.from(savedEnrollment);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollment(
            UUID enrollmentId
    ) {
        Enrollment enrollment = enrollmentRepository
                .findById(enrollmentId)
                .orElseThrow(
                        () -> new EnrollmentNotFoundException(
                                enrollmentId
                        )
                );

        return EnrollmentResponse.from(enrollment);
    }

    private void validateNoOpenEnrollment(
            UUID studentId,
            UUID batchId
    ) {
        boolean alreadyEnrolled =
                enrollmentRepository
                        .existsByStudentIdAndBatchIdAndStatusIn(
                                studentId,
                                batchId,
                                OPEN_ENROLLMENT_STATUSES
                        );

        if (alreadyEnrolled) {
            throw new EnrollmentConflictException(
                    "Student already has an open enrollment "
                            + "in this batch"
            );
        }
    }
}