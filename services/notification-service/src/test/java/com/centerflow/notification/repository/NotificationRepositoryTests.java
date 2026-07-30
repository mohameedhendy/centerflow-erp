package com.centerflow.notification.repository;

import com.centerflow.notification.domain.Notification;
import com.centerflow.notification.domain.NotificationStatus;
import com.centerflow.notification.domain.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationRepositoryTests {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void shouldSaveAndFindNotificationBySourceEventId() {
        UUID sourceEventId = UUID.randomUUID();

        Notification savedNotification =
                notificationRepository.saveAndFlush(
                        createNotification(
                                UUID.randomUUID(),
                                sourceEventId
                        )
                );

        Notification foundNotification =
                notificationRepository
                        .findBySourceEventId(sourceEventId)
                        .orElseThrow();

        assertThat(foundNotification.getId())
                .isEqualTo(savedNotification.getId());

        assertThat(
                notificationRepository
                        .existsBySourceEventId(
                                sourceEventId
                        )
        ).isTrue();
    }

    @Test
    void shouldFindNotificationsForRecipient() {
        UUID recipientUserId = UUID.randomUUID();

        notificationRepository.saveAndFlush(
                createNotification(
                        recipientUserId,
                        UUID.randomUUID()
                )
        );

        notificationRepository.saveAndFlush(
                createNotification(
                        recipientUserId,
                        UUID.randomUUID()
                )
        );

        notificationRepository.saveAndFlush(
                createNotification(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                )
        );

        Page<Notification> page =
                notificationRepository
                        .findAllByRecipientUserId(
                                recipientUserId,
                                PageRequest.of(
                                        0,
                                        10,
                                        Sort.by(
                                                Sort.Direction.DESC,
                                                "createdAt"
                                        )
                                )
                        );

        assertThat(page.getTotalElements())
                .isEqualTo(2);

        assertThat(page.getContent())
                .allMatch(notification ->
                        notification
                                .getRecipientUserId()
                                .equals(recipientUserId)
                );
    }

    @Test
    void shouldCountUnreadNotifications() {
        UUID recipientUserId = UUID.randomUUID();

        Notification unreadNotification =
                createNotification(
                        recipientUserId,
                        UUID.randomUUID()
                );

        Notification readNotification =
                createNotification(
                        recipientUserId,
                        UUID.randomUUID()
                );

        readNotification.markAsRead();

        notificationRepository.save(unreadNotification);
        notificationRepository.saveAndFlush(readNotification);

        long unreadCount = notificationRepository
                .countByRecipientUserIdAndStatus(
                        recipientUserId,
                        NotificationStatus.UNREAD
                );

        assertThat(unreadCount).isEqualTo(1);
    }

    @Test
    void statusChangeShouldBePersisted() {
        Notification notification =
                notificationRepository.saveAndFlush(
                        createNotification(
                                UUID.randomUUID(),
                                UUID.randomUUID()
                        )
                );

        notification.markAsRead();

        notificationRepository.flush();

        Notification updatedNotification =
                notificationRepository
                        .findById(notification.getId())
                        .orElseThrow();

        assertThat(updatedNotification.getStatus())
                .isEqualTo(NotificationStatus.READ);

        assertThat(updatedNotification.getReadAt())
                .isNotNull();
    }

    private Notification createNotification(
            UUID recipientUserId,
            UUID sourceEventId
    ) {
        return Notification.create(
                recipientUserId,
                NotificationType.GENERAL,
                "Test notification",
                "This is a repository test notification.",
                null,
                null,
                sourceEventId
        );
    }
}