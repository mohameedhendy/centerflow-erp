package com.centerflow.academic.branch.application;

import com.centerflow.academic.branch.domain.Branch;

import java.time.Instant;
import java.util.UUID;

public record BranchResult(
        UUID id,
        String code,
        String name,
        String phone,
        String email,
        String address,
        String city,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static BranchResult from(Branch branch) {
        return new BranchResult(
                branch.getId(),
                branch.getCode(),
                branch.getName(),
                branch.getPhone(),
                branch.getEmail(),
                branch.getAddress(),
                branch.getCity(),
                branch.isActive(),
                branch.getCreatedAt(),
                branch.getUpdatedAt()
        );
    }
}