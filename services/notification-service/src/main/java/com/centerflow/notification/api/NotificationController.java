package com.centerflow.notification.api;

import com.centerflow.notification.api.dto.CreateNotificationRequest;
import com.centerflow.notification.api.dto.NotificationCreationResponse;
import com.centerflow.notification.api.dto.NotificationResponse;
import com.centerflow.notification.api.dto.UnreadNotificationCountResponse;
import com.centerflow.notification.application.NotificationApplicationService;
import com.centerflow.notification.common.api.PageResponse;
import com.centerflow.notification.domain.NotificationStatus;
import com.centerflow.notification.domain.NotificationType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationApplicationService
            notificationService;

    public NotificationController(
            NotificationApplicationService
                    notificationService
    ) {
        this.notificationService =
                notificationService;
    }

    @PostMapping("/internal/notifications")
    public ResponseEntity<NotificationCreationResponse>
    createNotification(
            @Valid
            @RequestBody
            CreateNotificationRequest request
    ) {
        NotificationCreationResponse response =
                notificationService
                        .createNotification(request);

        HttpStatus responseStatus =
                response.created()
                        ? HttpStatus.CREATED
                        : HttpStatus.OK;

        return ResponseEntity
                .status(responseStatus)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<
            PageResponse<NotificationResponse>
            > searchNotifications(
            @RequestParam
            UUID recipientUserId,

            @RequestParam(required = false)
            NotificationStatus status,

            @RequestParam(required = false)
            NotificationType type,

            @RequestParam(required = false)
            String referenceType,

            @RequestParam(required = false)
            UUID referenceId,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        return ResponseEntity.ok(
                notificationService.searchNotifications(
                        recipientUserId,
                        status,
                        type,
                        referenceType,
                        referenceId,
                        page,
                        size
                )
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<
            UnreadNotificationCountResponse
            > getUnreadCount(
            @RequestParam
            UUID recipientUserId
    ) {
        return ResponseEntity.ok(
                notificationService.getUnreadCount(
                        recipientUserId
                )
        );
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponse>
    getNotification(
            @PathVariable
            UUID notificationId,

            @RequestParam
            UUID recipientUserId
    ) {
        return ResponseEntity.ok(
                notificationService.getNotification(
                        notificationId,
                        recipientUserId
                )
        );
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse>
    markAsRead(
            @PathVariable
            UUID notificationId,

            @RequestParam
            UUID recipientUserId
    ) {
        return ResponseEntity.ok(
                notificationService.markAsRead(
                        notificationId,
                        recipientUserId
                )
        );
    }

    @PostMapping("/{notificationId}/archive")
    public ResponseEntity<NotificationResponse>
    archive(
            @PathVariable
            UUID notificationId,

            @RequestParam
            UUID recipientUserId
    ) {
        return ResponseEntity.ok(
                notificationService.archive(
                        notificationId,
                        recipientUserId
                )
        );
    }
}