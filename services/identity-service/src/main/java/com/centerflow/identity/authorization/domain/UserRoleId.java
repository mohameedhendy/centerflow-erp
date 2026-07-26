package com.centerflow.identity.authorization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UserRoleId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    protected UserRoleId() {
    }

    private UserRoleId(UUID userId, UUID roleId) {
        this.userId = Objects.requireNonNull(
                userId,
                "User ID is required"
        );
        this.roleId = Objects.requireNonNull(
                roleId,
                "Role ID is required"
        );
    }

    public static UserRoleId of(UUID userId, UUID roleId) {
        return new UserRoleId(userId, roleId);
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof UserRoleId that)) {
            return false;
        }

        return Objects.equals(userId, that.userId)
                && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}