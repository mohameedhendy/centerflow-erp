package com.centerflow.academic.batch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Entity
@Table(name = "batches")
public class Batch {

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

    @Column(
            name = "branch_id",
            nullable = false
    )
    private UUID branchId;

    @Column(
            name = "classroom_id",
            nullable = false
    )
    private UUID classroomId;

    @Column(
            name = "course_level_id",
            nullable = false
    )
    private UUID courseLevelId;

    @Column(
            name = "instructor_id",
            nullable = false
    )
    private UUID instructorId;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(
            name = "start_date",
            nullable = false
    )
    private LocalDate startDate;

    @Column(
            name = "end_date",
            nullable = false
    )
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private BatchStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Batch() {
    }

    private Batch(
            String code,
            String name,
            UUID branchId,
            UUID classroomId,
            UUID courseLevelId,
            UUID instructorId,
            int capacity,
            LocalDate startDate,
            LocalDate endDate,
            Instant createdAt
    ) {
        this.code = normalizeCode(code);

        configure(
                name,
                branchId,
                classroomId,
                courseLevelId,
                instructorId,
                capacity,
                startDate,
                endDate
        );

        this.status = BatchStatus.DRAFT;

        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Creation time is required"
        );

        this.updatedAt = createdAt;
    }

    public static Batch create(
            String code,
            String name,
            UUID branchId,
            UUID classroomId,
            UUID courseLevelId,
            UUID instructorId,
            int capacity,
            LocalDate startDate,
            LocalDate endDate,
            Instant createdAt
    ) {
        return new Batch(
                code,
                name,
                branchId,
                classroomId,
                courseLevelId,
                instructorId,
                capacity,
                startDate,
                endDate,
                createdAt
        );
    }

    public void reconfigure(
            String name,
            UUID branchId,
            UUID classroomId,
            UUID courseLevelId,
            UUID instructorId,
            int capacity,
            LocalDate startDate,
            LocalDate endDate,
            Instant updatedAt
    ) {
        configure(
                name,
                branchId,
                classroomId,
                courseLevelId,
                instructorId,
                capacity,
                startDate,
                endDate
        );

        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "Update time is required"
        );
    }

    public void changeStatus(
            BatchStatus status,
            Instant updatedAt
    ) {
        if (this.status == status) {
            return;
        }

        this.status = Objects.requireNonNull(
                status,
                "Batch status is required"
        );

        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "Update time is required"
        );
    }

    private void configure(
            String name,
            UUID branchId,
            UUID classroomId,
            UUID courseLevelId,
            UUID instructorId,
            int capacity,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.name = normalizeRequiredText(
                name,
                "Batch name is required",
                150
        );

        this.branchId = Objects.requireNonNull(
                branchId,
                "Branch ID is required"
        );

        this.classroomId = Objects.requireNonNull(
                classroomId,
                "Classroom ID is required"
        );

        this.courseLevelId = Objects.requireNonNull(
                courseLevelId,
                "Course level ID is required"
        );

        this.instructorId = Objects.requireNonNull(
                instructorId,
                "Instructor ID is required"
        );

        this.capacity = validateCapacity(capacity);

        this.startDate = Objects.requireNonNull(
                startDate,
                "Batch start date is required"
        );

        this.endDate = Objects.requireNonNull(
                endDate,
                "Batch end date is required"
        );

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "Batch end date must not be before start date"
            );
        }
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

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getClassroomId() {
        return classroomId;
    }

    public UUID getCourseLevelId() {
        return courseLevelId;
    }

    public UUID getInstructorId() {
        return instructorId;
    }

    public int getCapacity() {
        return capacity;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BatchStatus getStatus() {
        return status;
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
                        "Batch code is required",
                        30
                ).toUpperCase(Locale.ROOT);

        if (!CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new IllegalArgumentException(
                    "Batch code may contain letters, numbers, and single hyphens only"
            );
        }

        return normalizedCode;
    }

    private static int validateCapacity(int capacity) {
        if (capacity < 1 || capacity > MAXIMUM_CAPACITY) {
            throw new IllegalArgumentException(
                    "Batch capacity must be between 1 and "
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
}