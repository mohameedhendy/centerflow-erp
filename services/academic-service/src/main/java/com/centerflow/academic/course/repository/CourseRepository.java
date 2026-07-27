package com.centerflow.academic.course.repository;

import com.centerflow.academic.course.domain.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CourseRepository
        extends JpaRepository<Course, UUID> {

    boolean existsByCode(String code);

    @Query("""
            SELECT course
            FROM Course course
            WHERE (
                :search IS NULL
                OR LOWER(course.code) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
                OR LOWER(course.name) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
            )
            AND (
                :active IS NULL
                OR course.active = :active
            )
            """)
    Page<Course> search(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );
}