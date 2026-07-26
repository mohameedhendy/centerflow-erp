package com.centerflow.identity.authorization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_roles")
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    protected UserRole() {
    }

    private UserRole(
            UserRoleId id,
            Instant assignedAt,
            UUID assignedBy
    ) {
        this.id = Objects.requireNonNull(
                id,
                "User role ID is required"
        );
        this.assignedAt = Objects.requireNonNull(
                assignedAt,
                "Assignment time is required"
        );
        this.assignedBy = assignedBy;
    }

    public static UserRole assign(
            UUID userId,
            UUID roleId,
            UUID assignedBy,
            Instant assignedAt
    ) {
        return new UserRole(
                UserRoleId.of(userId, roleId),
                assignedAt,
                assignedBy
        );
    }

    public UserRoleId getId() {
        return id;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }
}