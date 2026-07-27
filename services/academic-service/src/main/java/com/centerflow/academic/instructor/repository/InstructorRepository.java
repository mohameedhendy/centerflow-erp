package com.centerflow.academic.instructor.repository;

import com.centerflow.academic.instructor.domain.Instructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface InstructorRepository
        extends JpaRepository<Instructor, UUID> {

    boolean existsByCode(String code);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(
            String email,
            UUID instructorId
    );

    @Query("""
            SELECT instructor
            FROM Instructor instructor
            WHERE (
                :search IS NULL
                OR LOWER(instructor.code) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
                OR LOWER(instructor.firstName) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
                OR LOWER(instructor.lastName) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
                OR LOWER(
                    CONCAT(
                        CONCAT(
                            instructor.firstName,
                            ' '
                        ),
                        instructor.lastName
                    )
                ) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
                OR LOWER(instructor.email) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
                OR LOWER(instructor.phone) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
                OR LOWER(instructor.specialization) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
            )
            AND (
                :specialization IS NULL
                OR LOWER(instructor.specialization)
                    = LOWER(:specialization)
            )
            AND (
                :active IS NULL
                OR instructor.active = :active
            )
            """)
    Page<Instructor> search(
            @Param("search") String search,
            @Param("specialization")
            String specialization,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT instructor
        FROM Instructor instructor
        WHERE instructor.id = :instructorId
        """)
    Optional<Instructor> findByIdForUpdate(
            @Param("instructorId") UUID instructorId
    );
}