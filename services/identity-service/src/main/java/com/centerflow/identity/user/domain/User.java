package com.centerflow.identity.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    private User(
            String email,
            String passwordHash,
            UserStatus status,
            boolean emailVerified,
            Instant createdAt
    ) {
        this.email = normalizeEmail(email);
        this.passwordHash = requireText(passwordHash, "Password hash is required");
        this.status = Objects.requireNonNull(status, "User status is required");
        this.emailVerified = emailVerified;
        this.createdAt = Objects.requireNonNull(createdAt, "Creation time is required");
        this.updatedAt = createdAt;
    }

    public static User createPendingVerification(
            String email,
            String passwordHash,
            Instant createdAt
    ) {
        return new User(
                email,
                passwordHash,
                UserStatus.PENDING_VERIFICATION,
                false,
                createdAt
        );
    }

    public static User createActive(
            String email,
            String passwordHash,
            Instant createdAt
    ) {
        return new User(
                email,
                passwordHash,
                UserStatus.ACTIVE,
                true,
                createdAt
        );
    }

    public void verifyEmailAndActivate(Instant changedAt) {
        this.emailVerified = true;
        this.status = UserStatus.ACTIVE;
        this.updatedAt = requireTime(changedAt);
    }

    public void changePasswordHash(String newPasswordHash, Instant changedAt) {
        this.passwordHash = requireText(
                newPasswordHash,
                "Password hash is required"
        );
        this.updatedAt = requireTime(changedAt);
    }

    public void lock(Instant changedAt) {
        this.status = UserStatus.LOCKED;
        this.updatedAt = requireTime(changedAt);
    }

    public void disable(Instant changedAt) {
        this.status = UserStatus.DISABLED;
        this.updatedAt = requireTime(changedAt);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String normalizeEmail(String email) {
        String normalizedEmail = requireText(email, "Email is required")
                .toLowerCase(Locale.ROOT);

        if (normalizedEmail.length() > 320) {
            throw new IllegalArgumentException(
                    "Email must not exceed 320 characters"
            );
        }

        return normalizedEmail;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.strip();
    }

    private static Instant requireTime(Instant value) {
        return Objects.requireNonNull(value, "Change time is required");
    }
}