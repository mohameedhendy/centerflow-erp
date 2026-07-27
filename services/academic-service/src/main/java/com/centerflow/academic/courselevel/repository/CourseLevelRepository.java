package com.centerflow.academic.courselevel.repository;

import com.centerflow.academic.courselevel.domain.CourseLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CourseLevelRepository
        extends JpaRepository<CourseLevel, UUID> {

    boolean existsByCourseIdAndCode(
            UUID courseId,
            String code
    );

    boolean existsByCourseIdAndSequenceNumber(
            UUID courseId,
            int sequenceNumber
    );

    boolean existsByCourseIdAndSequenceNumberAndIdNot(
            UUID courseId,
            int sequenceNumber,
            UUID levelId
    );

    @Query("""
            SELECT level
            FROM CourseLevel level
            WHERE (
                :courseId IS NULL
                OR level.courseId = :courseId
            )
            AND (
                :search IS NULL
                OR LOWER(level.code) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
                OR LOWER(level.name) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
            )
            AND (
                :active IS NULL
                OR level.active = :active
            )
            """)
    Page<CourseLevel> search(
            @Param("courseId") UUID courseId,
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );
}