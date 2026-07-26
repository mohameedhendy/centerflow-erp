package com.centerflow.identity.security.password;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "user_id",
            nullable = false,
            updatable = false
    )
    private UUID userId;

    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            updatable = false,
            length = 128
    )
    private String tokenHash;

    @Column(
            name = "expires_at",
            nullable = false,
            updatable = false
    )
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    protected PasswordResetToken() {
    }

    private PasswordResetToken(
            UUID userId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt
    ) {
        this.userId = Objects.requireNonNull(
                userId,
                "User ID is required"
        );

        this.tokenHash = requireText(
                tokenHash,
                "Password reset token hash is required"
        );

        this.expiresAt = Objects.requireNonNull(
                expiresAt,
                "Password reset token expiry is required"
        );

        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Creation time is required"
        );

        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException(
                    "Password reset token expiry must be after creation time"
            );
        }
    }

    public static PasswordResetToken create(
            UUID userId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt
    ) {
        return new PasswordResetToken(
                userId,
                tokenHash,
                expiresAt,
                createdAt
        );
    }

    public boolean isActiveAt(Instant instant) {
        Objects.requireNonNull(
                instant,
                "Validation time is required"
        );

        return usedAt == null
                && revokedAt == null
                && expiresAt.isAfter(instant);
    }

    public void markUsed(Instant usedAt) {
        if (this.usedAt != null) {
            return;
        }

        this.usedAt = Objects.requireNonNull(
                usedAt,
                "Usage time is required"
        );
    }

    public void revoke(Instant revokedAt) {
        if (this.revokedAt != null
                || this.usedAt != null) {
            return;
        }

        this.revokedAt = Objects.requireNonNull(
                revokedAt,
                "Revocation time is required"
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }
}