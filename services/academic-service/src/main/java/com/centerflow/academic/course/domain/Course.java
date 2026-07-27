package com.centerflow.academic.course.domain;

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
import java.util.regex.Pattern;

@Entity
@Table(name = "courses")
public class Course {

    private static final Pattern CODE_PATTERN =
            Pattern.compile(
                    "^[A-Z0-9]+(?:-[A-Z0-9]+)*$"
            );

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "code",
            nullable = false,
            unique = true,
            updatable = false,
            length = 30
    )
    private String code;

    @Column(
            name = "name",
            nullable = false,
            length = 150
    )
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Course() {
    }

    private Course(
            String code,
            String name,
            String description,
            Instant createdAt
    ) {
        this.code = normalizeCode(code);

        this.name = normalizeRequiredText(
                name,
                "Course name is required",
                150
        );

        this.description =
                normalizeOptionalText(
                        description,
                        1000
                );

        this.active = true;

        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Creation time is required"
        );

        this.updatedAt = createdAt;
    }

    public static Course create(
            String code,
            String name,
            String description,
            Instant createdAt
    ) {
        return new Course(
                code,
                name,
                description,
                createdAt
        );
    }

    public void updateDetails(
            String name,
            String description,
            Instant updatedAt
    ) {
        this.name = normalizeRequiredText(
                name,
                "Course name is required",
                150
        );

        this.description =
                normalizeOptionalText(
                        description,
                        1000
                );

        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "Update time is required"
        );
    }

    public void changeStatus(
            boolean active,
            Instant changedAt
    ) {
        if (this.active == active) {
            return;
        }

        this.active = active;

        this.updatedAt = Objects.requireNonNull(
                changedAt,
                "Status change time is required"
        );
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public static String normalizedCode(String code) {
        return normalizeCode(code);
    }

    private static String normalizeCode(String code) {
        String normalizedCode =
                normalizeRequiredText(
                        code,
                        "Course code is required",
                        30
                ).toUpperCase(Locale.ROOT);

        if (!CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new IllegalArgumentException(
                    "Course code may contain letters, numbers, and single hyphens only"
            );
        }

        return normalizedCode;
    }

    private static String normalizeRequiredText(
            String value,
            String message,
            int maximumLength
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        String normalizedValue = value.strip();

        if (normalizedValue.length() > maximumLength) {
            throw new IllegalArgumentException(
                    "Value must not exceed "
                            + maximumLength
                            + " characters"
            );
        }

        return normalizedValue;
    }

    private static String normalizeOptionalText(
            String value,
            int maximumLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.strip();

        if (normalizedValue.length() > maximumLength) {
            throw new IllegalArgumentException(
                    "Value must not exceed "
                            + maximumLength
                            + " characters"
            );
        }

        return normalizedValue;
    }
}