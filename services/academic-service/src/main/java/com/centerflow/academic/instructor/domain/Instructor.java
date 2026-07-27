package com.centerflow.academic.instructor.domain;

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
@Table(name = "instructors")
public class Instructor {

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
            name = "first_name",
            nullable = false,
            length = 100
    )
    private String firstName;

    @Column(
            name = "last_name",
            nullable = false,
            length = 100
    )
    private String lastName;

    @Column(
            name = "email",
            unique = true,
            length = 320
    )
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(
            name = "specialization",
            length = 150
    )
    private String specialization;

    @Column(name = "bio", length = 1000)
    private String bio;

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

    protected Instructor() {
    }

    private Instructor(
            String code,
            String firstName,
            String lastName,
            String email,
            String phone,
            String specialization,
            String bio,
            Instant createdAt
    ) {
        this.code = normalizeCode(code);

        this.firstName = normalizeRequiredText(
                firstName,
                "Instructor first name is required",
                100
        );

        this.lastName = normalizeRequiredText(
                lastName,
                "Instructor last name is required",
                100
        );

        this.email = normalizeEmail(email);
        this.phone = normalizeOptionalText(phone, 30);

        this.specialization = normalizeOptionalText(
                specialization,
                150
        );

        this.bio = normalizeOptionalText(bio, 1000);
        this.active = true;

        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Creation time is required"
        );

        this.updatedAt = createdAt;
    }

    public static Instructor create(
            String code,
            String firstName,
            String lastName,
            String email,
            String phone,
            String specialization,
            String bio,
            Instant createdAt
    ) {
        return new Instructor(
                code,
                firstName,
                lastName,
                email,
                phone,
                specialization,
                bio,
                createdAt
        );
    }

    public void updateDetails(
            String firstName,
            String lastName,
            String email,
            String phone,
            String specialization,
            String bio,
            Instant updatedAt
    ) {
        this.firstName = normalizeRequiredText(
                firstName,
                "Instructor first name is required",
                100
        );

        this.lastName = normalizeRequiredText(
                lastName,
                "Instructor last name is required",
                100
        );

        this.email = normalizeEmail(email);
        this.phone = normalizeOptionalText(phone, 30);

        this.specialization = normalizeOptionalText(
                specialization,
                150
        );

        this.bio = normalizeOptionalText(bio, 1000);

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

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getSpecialization() {
        return specialization;
    }

    public String getBio() {
        return bio;
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

    public static String normalizedEmail(String email) {
        return normalizeEmail(email);
    }

    private static String normalizeCode(String code) {
        String normalizedCode =
                normalizeRequiredText(
                        code,
                        "Instructor code is required",
                        30
                ).toUpperCase(Locale.ROOT);

        if (!CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new IllegalArgumentException(
                    "Instructor code may contain letters, numbers, and single hyphens only"
            );
        }

        return normalizedCode;
    }

    private static String normalizeEmail(String email) {
        String normalizedEmail =
                normalizeOptionalText(email, 320);

        if (normalizedEmail == null) {
            return null;
        }

        return normalizedEmail.toLowerCase(
                Locale.ROOT
        );
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