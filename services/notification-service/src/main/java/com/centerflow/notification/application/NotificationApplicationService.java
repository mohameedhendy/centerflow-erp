package com.centerflow.notification.application;

import com.centerflow.notification.api.dto.CreateNotificationRequest;
import com.centerflow.notification.api.dto.NotificationCreationResponse;
import com.centerflow.notification.api.dto.NotificationResponse;
import com.centerflow.notification.api.dto.UnreadNotificationCountResponse;
import com.centerflow.notification.common.api.PageResponse;
import com.centerflow.notification.domain.Notification;
import com.centerflow.notification.domain.NotificationStatus;
import com.centerflow.notification.domain.NotificationType;
import com.centerflow.notification.exception.InvalidPaginationException;
import com.centerflow.notification.exception.NotificationNotFoundException;
import com.centerflow.notification.repository.NotificationRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationApplicationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository
            notificationRepository;

    public NotificationApplicationService(
            NotificationRepository notificationRepository
    ) {
        this.notificationRepository =
                notificationRepository;
    }

    public NotificationCreationResponse createNotification(
            CreateNotificationRequest request
    ) {
        UUID sourceEventId = request.sourceEventId();

        if (sourceEventId != null) {
            Optional<Notification> existingNotification =
                    notificationRepository
                            .findBySourceEventId(
                                    sourceEventId
                            );

            if (existingNotification.isPresent()) {
                return new NotificationCreationResponse(
                        NotificationResponse.from(
                                existingNotification.get()
                        ),
                        false
                );
            }
        }

        Notification notification = Notification.create(
                request.recipientUserId(),
                request.type(),
                request.title(),
                request.message(),
                request.referenceType(),
                request.referenceId(),
                sourceEventId
        );

        try {
            Notification savedNotification =
                    notificationRepository.saveAndFlush(
                            notification
                    );

            return new NotificationCreationResponse(
                    NotificationResponse.from(
                            savedNotification
                    ),
                    true
            );
        }
        catch (DataIntegrityViolationException exception) {
            if (sourceEventId == null) {
                throw exception;
            }

            Notification existingNotification =
                    notificationRepository
                            .findBySourceEventId(
                                    sourceEventId
                            )
                            .orElseThrow(
                                    () -> exception
                            );

            return new NotificationCreationResponse(
                    NotificationResponse.from(
                            existingNotification
                    ),
                    false
            );
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse>
    searchNotifications(
            UUID recipientUserId,
            NotificationStatus status,
            NotificationType type,
            String referenceType,
            UUID referenceId,
            int page,
            int size
    ) {
        validatePagination(page, size);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Specification<Notification> specification =
                buildSpecification(
                        recipientUserId,
                        status,
                        type,
                        referenceType,
                        referenceId
                );

        Page<Notification> notificationPage =
                notificationRepository.findAll(
                        specification,
                        pageRequest
                );

        return PageResponse.from(
                notificationPage,
                NotificationResponse::from
        );
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotification(
            UUID notificationId,
            UUID recipientUserId
    ) {
        Notification notification =
                notificationRepository
                        .findByIdAndRecipientUserId(
                                notificationId,
                                recipientUserId
                        )
                        .orElseThrow(
                                () ->
                                        new NotificationNotFoundException(
                                                notificationId
                                        )
                        );

        return NotificationResponse.from(notification);
    }

    @Transactional
    public NotificationResponse markAsRead(
            UUID notificationId,
            UUID recipientUserId
    ) {
        Notification notification =
                findOwnedNotificationForUpdate(
                        notificationId,
                        recipientUserId
                );

        notification.markAsRead();

        return NotificationResponse.from(notification);
    }

    @Transactional
    public NotificationResponse archive(
            UUID notificationId,
            UUID recipientUserId
    ) {
        Notification notification =
                findOwnedNotificationForUpdate(
                        notificationId,
                        recipientUserId
                );

        notification.archive();

        return NotificationResponse.from(notification);
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse
    getUnreadCount(UUID recipientUserId) {
        long unreadCount = notificationRepository
                .countByRecipientUserIdAndStatus(
                        recipientUserId,
                        NotificationStatus.UNREAD
                );

        return new UnreadNotificationCountResponse(
                recipientUserId,
                unreadCount
        );
    }

    private Notification findOwnedNotificationForUpdate(
            UUID notificationId,
            UUID recipientUserId
    ) {
        return notificationRepository
                .findOwnedByIdForUpdate(
                        notificationId,
                        recipientUserId
                )
                .orElseThrow(
                        () ->
                                new NotificationNotFoundException(
                                        notificationId
                                )
                );
    }

    private Specification<Notification>
    buildSpecification(
            UUID recipientUserId,
            NotificationStatus status,
            NotificationType type,
            String referenceType,
            UUID referenceId
    ) {
        return (root, query, criteriaBuilder) -> {
            ArrayList<Predicate> predicates =
                    new ArrayList<>();

            predicates.add(
                    criteriaBuilder.equal(
                            root.get("recipientUserId"),
                            recipientUserId
                    )
            );

            if (status != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("status"),
                                status
                        )
                );
            }

            if (type != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("type"),
                                type
                        )
                );
            }

            if (
                    referenceType != null
                            && !referenceType.isBlank()
            ) {
                String normalizedReferenceType =
                        referenceType
                                .trim()
                                .toUpperCase(Locale.ROOT);

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("referenceType"),
                                normalizedReferenceType
                        )
                );
            }

            if (referenceId != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("referenceId"),
                                referenceId
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new InvalidPaginationException(
                    "Page index must be zero or greater"
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPaginationException(
                    "Page size must be between 1 and "
                            + MAX_PAGE_SIZE
            );
        }
    }
}