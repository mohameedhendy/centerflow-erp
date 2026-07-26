package com.centerflow.identity.authorization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Permission() {
    }

    private Permission(
            String name,
            String description,
            Instant createdAt
    ) {
        this.name = normalizeName(name);
        this.description = normalizeDescription(description);
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Creation time is required"
        );
        this.updatedAt = createdAt;
    }

    public static Permission create(
            String name,
            String description,
            Instant createdAt
    ) {
        return new Permission(name, description, createdAt);
    }

    public void updateDescription(
            String description,
            Instant changedAt
    ) {
        this.description = normalizeDescription(description);
        this.updatedAt = Objects.requireNonNull(
                changedAt,
                "Change time is required"
        );
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Permission name is required"
            );
        }

        String normalizedName = name
                .strip()
                .toUpperCase(Locale.ROOT);

        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException(
                    "Permission name must not exceed 100 characters"
            );
        }

        return normalizedName;
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String normalizedDescription = description.strip();

        if (normalizedDescription.length() > 255) {
            throw new IllegalArgumentException(
                    "Permission description must not exceed 255 characters"
            );
        }

        return normalizedDescription;
    }
}