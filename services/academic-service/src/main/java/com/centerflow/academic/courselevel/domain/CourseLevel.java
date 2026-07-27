package com.centerflow.academic.courselevel.domain;

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
@Table(name = "course_levels")
public class CourseLevel {

    private static final int MAXIMUM_SEQUENCE = 100;
    private static final int MAXIMUM_DURATION_HOURS = 2000;

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
            name = "course_id",
            nullable = false,
            updatable = false
    )
    private UUID courseId;

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

    @Column(
            name = "sequence_number",
            nullable = false
    )
    private int sequenceNumber;

    @Column(
            name = "duration_hours",
            nullable = false
    )
    private int durationHours;

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

    protected CourseLevel() {
    }

    private CourseLevel(
            UUID courseId,
            String code,
            String name,
            int sequenceNumber,
            int durationHours,
            String description,
            Instant createdAt
    ) {
        this.courseId = Objects.requireNonNull(
                courseId,
                "Course ID is required"
        );

        this.code = normalizeCode(code);

        this.name = normalizeRequiredText(
                name,
                "Course level name is required",
                150
        );

        this.sequenceNumber =
                validateSequenceNumber(
                        sequenceNumber
                );

        this.durationHours =
                validateDurationHours(
                        durationHours
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

    public static CourseLevel create(
            UUID courseId,
            String code,
            String name,
            int sequenceNumber,
            int durationHours,
            String description,
            Instant createdAt
    ) {
        return new CourseLevel(
                courseId,
                code,
                name,
                sequenceNumber,
                durationHours,
                description,
                createdAt
        );
    }

    public void updateDetails(
            String name,
            int sequenceNumber,
            int durationHours,
            String description,
            Instant updatedAt
    ) {
        this.name = normalizeRequiredText(
                name,
                "Course level name is required",
                150
        );

        this.sequenceNumber =
                validateSequenceNumber(
                        sequenceNumber
                );

        this.durationHours =
                validateDurationHours(
                        durationHours
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

    public UUID getCourseId() {
        return courseId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public int getDurationHours() {
        return durationHours;
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
                        "Course level code is required",
                        30
                ).toUpperCase(Locale.ROOT);

        if (!CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new IllegalArgumentException(
                    "Course level code may contain letters, numbers, and single hyphens only"
            );
        }

        return normalizedCode;
    }

    private static int validateSequenceNumber(
            int sequenceNumber
    ) {
        if (sequenceNumber < 1
                || sequenceNumber > MAXIMUM_SEQUENCE) {
            throw new IllegalArgumentException(
                    "Course level sequence number must be between 1 and "
                            + MAXIMUM_SEQUENCE
            );
        }

        return sequenceNumber;
    }

    private static int validateDurationHours(
            int durationHours
    ) {
        if (durationHours < 1
                || durationHours
                > MAXIMUM_DURATION_HOURS) {
            throw new IllegalArgumentException(
                    "Course level duration must be between 1 and "
                            + MAXIMUM_DURATION_HOURS
                            + " hours"
            );
        }

        return durationHours;
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