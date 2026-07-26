package com.centerflow.identity.authorization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, length = 50)
    private RoleName name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Role() {
    }

    private Role(
            RoleName name,
            String description,
            Instant createdAt
    ) {
        this.name = Objects.requireNonNull(name, "Role name is required");
        this.description = normalizeDescription(description);
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Creation time is required"
        );
        this.updatedAt = createdAt;
    }

    public static Role create(
            RoleName name,
            String description,
            Instant createdAt
    ) {
        return new Role(name, description, createdAt);
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

    public RoleName getName() {
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

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String normalizedDescription = description.strip();

        if (normalizedDescription.length() > 255) {
            throw new IllegalArgumentException(
                    "Role description must not exceed 255 characters"
            );
        }

        return normalizedDescription;
    }
}