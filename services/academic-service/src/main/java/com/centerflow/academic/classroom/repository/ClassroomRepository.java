package com.centerflow.academic.classroom.repository;

import com.centerflow.academic.classroom.domain.Classroom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ClassroomRepository
        extends JpaRepository<Classroom, UUID> {

    boolean existsByBranchIdAndCode(
            UUID branchId,
            String code
    );

    @Query("""
            SELECT classroom
            FROM Classroom classroom
            WHERE (
                :branchId IS NULL
                OR classroom.branchId = :branchId
            )
            AND (
                :search IS NULL
                OR LOWER(classroom.code) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
                OR LOWER(classroom.name) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
            )
            AND (
                :minimumCapacity IS NULL
                OR classroom.capacity >= :minimumCapacity
            )
            AND (
                :maximumCapacity IS NULL
                OR classroom.capacity <= :maximumCapacity
            )
            AND (
                :active IS NULL
                OR classroom.active = :active
            )
            """)
    Page<Classroom> search(
            @Param("branchId") UUID branchId,
            @Param("search") String search,
            @Param("minimumCapacity")
            Integer minimumCapacity,
            @Param("maximumCapacity")
            Integer maximumCapacity,
            @Param("active") Boolean active,
            Pageable pageable
    );
}