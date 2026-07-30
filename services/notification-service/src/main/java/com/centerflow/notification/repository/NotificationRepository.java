package com.centerflow.notification.repository;

import com.centerflow.notification.domain.Notification;
import com.centerflow.notification.domain.NotificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    Optional<Notification>
    findByIdAndRecipientUserId(
            UUID id,
            UUID recipientUserId
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT notification
            FROM Notification notification
            WHERE notification.id = :notificationId
              AND notification.recipientUserId
                    = :recipientUserId
            """)
    Optional<Notification>
    findOwnedByIdForUpdate(
            @Param("notificationId")
            UUID notificationId,

            @Param("recipientUserId")
            UUID recipientUserId
    );
}