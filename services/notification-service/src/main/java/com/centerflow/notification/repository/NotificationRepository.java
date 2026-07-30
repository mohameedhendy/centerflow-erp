package com.centerflow.notification.repository;

import com.centerflow.notification.domain.Notification;
import com.centerflow.notification.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, UUID>,
        JpaSpecificationExecutor<Notification> {

    Optional<Notification> findBySourceEventId(
            UUID sourceEventId
    );

    boolean existsBySourceEventId(
            UUID sourceEventId
    );

    Page<Notification> findAllByRecipientUserId(
            UUID recipientUserId,
            Pageable pageable
    );

    Page<Notification>
    findAllByRecipientUserIdAndStatus(
            UUID recipientUserId,
            NotificationStatus status,
            Pageable pageable
    );

    long countByRecipientUserIdAndStatus(
            UUID recipientUserId,
            NotificationStatus status
    );
}