package com.centerflow.enrollment.repository;

import com.centerflow.enrollment.domain.Enrollment;
import com.centerflow.enrollment.domain.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
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

    @Query("""
            SELECT enrollment
            FROM Enrollment enrollment
            WHERE (
                :studentId IS NULL
                OR enrollment.studentId = :studentId
            )
            AND (
                :batchId IS NULL
                OR enrollment.batchId = :batchId
            )
            AND (
                :status IS NULL
                OR enrollment.status = :status
            )
            AND (
                COALESCE(:enrollmentNumber, '') = ''
                OR LOWER(enrollment.enrollmentNumber)
                    LIKE LOWER(
                        CONCAT(
                            '%',
                            COALESCE(:enrollmentNumber, ''),
                            '%'
                        )
                    )
            )
            """)
    Page<Enrollment> search(
            @Param("enrollmentNumber")
            String enrollmentNumber,

            @Param("studentId")
            UUID studentId,

            @Param("batchId")
            UUID batchId,

            @Param("status")
            EnrollmentStatus status,

            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT enrollment
        FROM Enrollment enrollment
        WHERE enrollment.id = :enrollmentId
        """)
    Optional<Enrollment> findByIdForUpdate(
            @Param("enrollmentId")
            UUID enrollmentId
    );

    boolean existsByStudentIdAndBatchIdAndStatusInAndIdNot(
            UUID studentId,
            UUID batchId,
            Collection<EnrollmentStatus> statuses,
            UUID enrollmentId
    );
}