package com.centerflow.identity.authorization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class RolePermissionId implements Serializable {

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;

    protected RolePermissionId() {
    }

    private RolePermissionId(
            UUID roleId,
            UUID permissionId
    ) {
        this.roleId = Objects.requireNonNull(
                roleId,
                "Role ID is required"
        );
        this.permissionId = Objects.requireNonNull(
                permissionId,
                "Permission ID is required"
        );
    }

    public static RolePermissionId of(
            UUID roleId,
            UUID permissionId
    ) {
        return new RolePermissionId(roleId, permissionId);
    }

    public UUID getRoleId() {
        return roleId;
    }

    public UUID getPermissionId() {
        return permissionId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof RolePermissionId that)) {
            return false;
        }

        return Objects.equals(roleId, that.roleId)
                && Objects.equals(permissionId, that.permissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, permissionId);
    }
}