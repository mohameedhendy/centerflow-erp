package com.centerflow.academic.branch.repository;

import com.centerflow.academic.branch.domain.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BranchRepository
        extends JpaRepository<Branch, UUID> {

    Optional<Branch> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
        SELECT branch
        FROM Branch branch
        WHERE (
            COALESCE(:keyword, '') = ''
            OR LOWER(branch.code) LIKE LOWER(
                CONCAT(
                    '%',
                    COALESCE(:keyword, ''),
                    '%'
                )
            )
            OR LOWER(branch.name) LIKE LOWER(
                CONCAT(
                    '%',
                    COALESCE(:keyword, ''),
                    '%'
                )
            )
        )
        AND (
            COALESCE(:city, '') = ''
            OR LOWER(branch.city) =
               LOWER(COALESCE(:city, ''))
        )
        AND (
            :active IS NULL
            OR branch.active = :active
        )
        """)
    Page<Branch> search(
            @Param("keyword") String keyword,
            @Param("city") String city,
            @Param("active") Boolean active,
            Pageable pageable
    );
}