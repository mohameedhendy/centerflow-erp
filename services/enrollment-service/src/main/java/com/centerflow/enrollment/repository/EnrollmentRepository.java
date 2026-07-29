package com.centerflow.enrollment.repository;

import com.centerflow.enrollment.domain.Enrollment;
import com.centerflow.enrollment.domain.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository
        extends JpaRepository<Enrollment, UUID> {

    Optional<Enrollment> findByEnrollmentNumber(
            String enrollmentNumber
    );

    boolean existsByEnrollmentNumber(
            String enrollmentNumber
    );

    boolean existsByStudentIdAndBatchIdAndStatusIn(
            UUID studentId,
            UUID batchId,
            Collection<EnrollmentStatus> statuses
    );

    Page<Enrollment> findAllByStudentId(
            UUID studentId,
            Pageable pageable
    );

    Page<Enrollment> findAllByBatchId(
            UUID batchId,
            Pageable pageable
    );

    Page<Enrollment> findAllByBatchIdAndStatus(
            UUID batchId,
            EnrollmentStatus status,
            Pageable pageable
    );
}