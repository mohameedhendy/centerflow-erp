package com.centerflow.enrollment.repository;

import com.centerflow.enrollment.domain.EnrollmentTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EnrollmentTransferRepository
        extends JpaRepository<EnrollmentTransfer, UUID> {

    Page<EnrollmentTransfer> findAllByEnrollmentId(
            UUID enrollmentId,
            Pageable pageable
    );
}