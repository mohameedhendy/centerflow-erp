package com.centerflow.academic.branch.api;

import com.centerflow.academic.branch.application.BranchResult;

import java.time.Instant;
import java.util.UUID;

public record BranchResponse(
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

    public static BranchResponse from(
            BranchResult result
    ) {
        return new BranchResponse(
                result.id(),
                result.code(),
                result.name(),
                result.phone(),
                result.email(),
                result.address(),
                result.city(),
                result.active(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}