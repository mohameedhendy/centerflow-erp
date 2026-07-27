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
                :search IS NULL
                OR LOWER(branch.code) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
                OR LOWER(branch.name) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
            )
            AND (
                :city IS NULL
                OR LOWER(branch.city) = LOWER(:city)
            )
            AND (
                :active IS NULL
                OR branch.active = :active
            )
            """)
    Page<Branch> search(
            @Param("search") String search,
            @Param("city") String city,
            @Param("active") Boolean active,
            Pageable pageable
    );
}