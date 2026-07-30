package com.centerflow.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(
            name = "recipient_user_id",
            nullable = false
    )
    private UUID recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "type",
            nullable = false,
            length = 50
    )
    private NotificationType type;

    @Column(
            name = "title",
            nullable = false,
            length = 150
    )
    private String title;

    @Column(
            name = "message",
            nullable = false,
            length = 1000
    )
    private String message;

    @Column(
            name = "reference_type",
            length = 50
    )
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(
            name = "source_event_id",
            unique = true
    )
    private UUID sourceEventId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Notification() {
    }

    private Notification(
            UUID id,
            UUID recipientUserId,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            UUID referenceId,
            UUID sourceEventId,
            NotificationStatus status,
            Instant createdAt,
            Instant readAt,
            Instant archivedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.recipientUserId =
                Objects.requireNonNull(recipientUserId);
        this.type = Objects.requireNonNull(type);
        this.title = normalizeRequiredText(
                title,
                "Notification title",
                150
        );
        this.message = normalizeRequiredText(
                message,
                "Notification message",
                1000
        );

        validateReference(
                referenceType,
                referenceId
        );

        this.referenceType =
                normalizeReferenceType(referenceType);
        this.referenceId = referenceId;
        this.sourceEventId = sourceEventId;
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.readAt = readAt;
        this.archivedAt = archivedAt;
    }

    public static Notification create(
            UUID recipientUserId,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            UUID referenceId,
            UUID sourceEventId
    ) {
        return new Notification(
                UUID.randomUUID(),
                recipientUserId,
                type,
                title,
                message,
                referenceType,
                referenceId,
                sourceEventId,
                NotificationStatus.UNREAD,
                Instant.now(),
                null,
                null
        );
    }

    public void markAsRead() {
        if (status == NotificationStatus.ARCHIVED) {
            throw new InvalidNotificationStateException(
                    "Archived notification cannot be marked as read"
            );
        }

        if (status == NotificationStatus.READ) {
            return;
        }

        status = NotificationStatus.READ;
        readAt = Instant.now();
    }

    public void archive() {
        if (status == NotificationStatus.ARCHIVED) {
            return;
        }

        status = NotificationStatus.ARCHIVED;
        archivedAt = Instant.now();
    }

    private static void validateReference(
            String referenceType,
            UUID referenceId
    ) {
        boolean hasReferenceType =
                referenceType != null
                        && !referenceType.isBlank();

        boolean hasReferenceId =
                referenceId != null;

        if (hasReferenceType != hasReferenceId) {
            throw new InvalidNotificationException(
                    "Reference type and reference ID "
                            + "must be provided together"
            );
        }
    }

    private static String normalizeReferenceType(
            String referenceType
    ) {
        if (
                referenceType == null
                        || referenceType.isBlank()
        ) {
            return null;
        }

        String normalized = referenceType
                .trim()
                .toUpperCase(Locale.ROOT);

        if (normalized.length() > 50) {
            throw new InvalidNotificationException(
                    "Reference type must not exceed "
                            + "50 characters"
            );
        }

        return normalized;
    }

    private static String normalizeRequiredText(
            String value,
            String fieldName,
            int maximumLength
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidNotificationException(
                    fieldName + " is required"
            );
        }

        String normalized = value.trim();

        if (normalized.length() > maximumLength) {
            throw new InvalidNotificationException(
                    fieldName
                            + " must not exceed "
                            + maximumLength
                            + " characters"
            );
        }

        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public long getVersion() {
        return version;
    }
}