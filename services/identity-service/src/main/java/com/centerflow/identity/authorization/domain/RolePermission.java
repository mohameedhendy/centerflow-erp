package com.centerflow.identity.authorization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "role_permissions")
public class RolePermission {

    @EmbeddedId
    private RolePermissionId id;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    protected RolePermission() {
    }

    private RolePermission(
            RolePermissionId id,
            Instant assignedAt,
            UUID assignedBy
    ) {
        this.id = Objects.requireNonNull(
                id,
                "Role permission ID is required"
        );
        this.assignedAt = Objects.requireNonNull(
                assignedAt,
                "Assignment time is required"
        );
        this.assignedBy = assignedBy;
    }

    public static RolePermission assign(
            UUID roleId,
            UUID permissionId,
            UUID assignedBy,
            Instant assignedAt
    ) {
        return new RolePermission(
                RolePermissionId.of(roleId, permissionId),
                assignedAt,
                assignedBy
        );
    }

    public RolePermissionId getId() {
        return id;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }
}