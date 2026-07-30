package com.centerflow.notification.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTests {

    @Test
    void createShouldInitializeUnreadNotification() {
        UUID recipientUserId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        UUID sourceEventId = UUID.randomUUID();

        Notification notification = Notification.create(
                recipientUserId,
                NotificationType.PAYMENT_RECORDED,
                "  Payment received  ",
                "  Your payment was recorded successfully.  ",
                " enrollment ",
                referenceId,
                sourceEventId
        );

        assertThat(notification.getId()).isNotNull();

        assertThat(notification.getRecipientUserId())
                .isEqualTo(recipientUserId);

        assertThat(notification.getType())
                .isEqualTo(
                        NotificationType.PAYMENT_RECORDED
                );

        assertThat(notification.getTitle())
                .isEqualTo("Payment received");

        assertThat(notification.getMessage())
                .isEqualTo(
                        "Your payment was recorded successfully."
                );

        assertThat(notification.getReferenceType())
                .isEqualTo("ENROLLMENT");

        assertThat(notification.getReferenceId())
                .isEqualTo(referenceId);

        assertThat(notification.getSourceEventId())
                .isEqualTo(sourceEventId);

        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.UNREAD);

        assertThat(notification.getCreatedAt())
                .isNotNull();

        assertThat(notification.getReadAt()).isNull();
        assertThat(notification.getArchivedAt()).isNull();
    }

    @Test
    void createShouldAllowNotificationWithoutReference() {
        Notification notification = Notification.create(
                UUID.randomUUID(),
                NotificationType.GENERAL,
                "Center announcement",
                "Tomorrow is a public holiday.",
                null,
                null,
                null
        );

        assertThat(notification.getReferenceType())
                .isNull();

        assertThat(notification.getReferenceId())
                .isNull();

        assertThat(notification.getSourceEventId())
                .isNull();
    }

    @Test
    void createShouldRejectIncompleteReference() {
        assertThatThrownBy(
                () -> Notification.create(
                        UUID.randomUUID(),
                        NotificationType.ENROLLMENT_CREATED,
                        "Enrollment created",
                        "Your enrollment was created.",
                        "ENROLLMENT",
                        null,
                        UUID.randomUUID()
                )
        )
                .isInstanceOf(
                        InvalidNotificationException.class
                )
                .hasMessage(
                        "Reference type and reference ID "
                                + "must be provided together"
                );
    }

    @Test
    void markAsReadShouldBeIdempotent() {
        Notification notification = createNotification();

        notification.markAsRead();

        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.READ);

        assertThat(notification.getReadAt())
                .isNotNull();

        var originalReadAt = notification.getReadAt();

        notification.markAsRead();

        assertThat(notification.getReadAt())
                .isEqualTo(originalReadAt);
    }

    @Test
    void archiveShouldBeIdempotent() {
        Notification notification = createNotification();

        notification.archive();

        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.ARCHIVED);

        assertThat(notification.getArchivedAt())
                .isNotNull();

        var originalArchivedAt =
                notification.getArchivedAt();

        notification.archive();

        assertThat(notification.getArchivedAt())
                .isEqualTo(originalArchivedAt);
    }

    @Test
    void archivedNotificationCannotBeMarkedAsRead() {
        Notification notification = createNotification();

        notification.archive();

        assertThatThrownBy(notification::markAsRead)
                .isInstanceOf(
                        InvalidNotificationStateException.class
                )
                .hasMessage(
                        "Archived notification cannot "
                                + "be marked as read"
                );
    }

    private Notification createNotification() {
        return Notification.create(
                UUID.randomUUID(),
                NotificationType.GENERAL,
                "Test notification",
                "Notification message",
                null,
                null,
                UUID.randomUUID()
        );
    }
}