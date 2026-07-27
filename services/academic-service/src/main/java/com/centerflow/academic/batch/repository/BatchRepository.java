package com.centerflow.academic.batch.repository;

import com.centerflow.academic.batch.domain.Batch;
import com.centerflow.academic.batch.domain.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface BatchRepository
        extends JpaRepository<Batch, UUID> {

    boolean existsByCode(String code);

    @Query("""
            SELECT batch
            FROM Batch batch
            WHERE (
                :branchId IS NULL
                OR batch.branchId = :branchId
            )
            AND (
                :classroomId IS NULL
                OR batch.classroomId = :classroomId
            )
            AND (
                :courseLevelId IS NULL
                OR batch.courseLevelId = :courseLevelId
            )
            AND (
                :instructorId IS NULL
                OR batch.instructorId = :instructorId
            )
            AND (
                :status IS NULL
                OR batch.status = :status
            )
            AND (
                :search IS NULL
                OR LOWER(batch.code) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
                OR LOWER(batch.name) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
            )
            AND (
                :startDateFrom IS NULL
                OR batch.startDate >= :startDateFrom
            )
            AND (
                :startDateTo IS NULL
                OR batch.startDate <= :startDateTo
            )
            """)
    Page<Batch> search(
            @Param("branchId") UUID branchId,
            @Param("classroomId") UUID classroomId,
            @Param("courseLevelId") UUID courseLevelId,
            @Param("instructorId") UUID instructorId,
            @Param("status") BatchStatus status,
            @Param("search") String search,
            @Param("startDateFrom")
            LocalDate startDateFrom,
            @Param("startDateTo")
            LocalDate startDateTo,
            Pageable pageable
    );
}