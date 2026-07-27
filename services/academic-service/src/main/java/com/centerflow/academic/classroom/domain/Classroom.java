package com.centerflow.academic.classroom.domain;

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
@Table(name = "classrooms")
public class Classroom {

    private static final int MAXIMUM_CAPACITY = 1000;

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
            name = "branch_id",
            nullable = false,
            updatable = false
    )
    private UUID branchId;

    @Column(
            name = "code",
            nullable = false,
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

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "floor", length = 50)
    private String floor;

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

    protected Classroom() {
    }

    private Classroom(
            UUID branchId,
            String code,
            String name,
            int capacity,
            String floor,
            Instant createdAt
    ) {
        this.branchId = Objects.requireNonNull(
                branchId,
                "Branch ID is required"
        );

        this.code = normalizeCode(code);

        this.name = normalizeRequiredText(
                name,
                "Classroom name is required",
                150
        );

        this.capacity = validateCapacity(capacity);
        this.floor = normalizeOptionalText(floor, 50);
        this.active = true;

        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Creation time is required"
        );

        this.updatedAt = createdAt;
    }

    public static Classroom create(
            UUID branchId,
            String code,
            String name,
            int capacity,
            String floor,
            Instant createdAt
    ) {
        return new Classroom(
                branchId,
                code,
                name,
                capacity,
                floor,
                createdAt
        );
    }

    public void updateDetails(
            String name,
            int capacity,
            String floor,
            Instant updatedAt
    ) {
        this.name = normalizeRequiredText(
                name,
                "Classroom name is required",
                150
        );

        this.capacity = validateCapacity(capacity);
        this.floor = normalizeOptionalText(floor, 50);

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

    public UUID getBranchId() {
        return branchId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getFloor() {
        return floor;
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
                        "Classroom code is required",
                        30
                ).toUpperCase(Locale.ROOT);

        if (!CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new IllegalArgumentException(
                    "Classroom code may contain letters, numbers, and single hyphens only"
            );
        }

        return normalizedCode;
    }

    private static int validateCapacity(int capacity) {
        if (capacity < 1 || capacity > MAXIMUM_CAPACITY) {
            throw new IllegalArgumentException(
                    "Classroom capacity must be between 1 and "
                            + MAXIMUM_CAPACITY
            );
        }

        return capacity;
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