package com.centerflow.notification.api;

import com.centerflow.notification.domain.Notification;
import com.centerflow.notification.domain.NotificationType;
import com.centerflow.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void createShouldBeIdempotentBySourceEventId()
            throws Exception {
        UUID recipientUserId = UUID.randomUUID();
        UUID sourceEventId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();

        String requestBody = """
                {
                  "recipientUserId": "%s",
                  "type": "PAYMENT_RECORDED",
                  "title": "Payment received",
                  "message": "Your payment was recorded.",
                  "referenceType": "ENROLLMENT",
                  "referenceId": "%s",
                  "sourceEventId": "%s"
                }
                """.formatted(
                recipientUserId,
                referenceId,
                sourceEventId
        );

        mockMvc.perform(
                        post(
                                "/api/v1/notifications/internal/notifications"
                        )
                                .contentType("application/json")
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.created").value(true)
                )
                .andExpect(
                        jsonPath(
                                "$.notification.status"
                        ).value("UNREAD")
                );

        mockMvc.perform(
                        post(
                                "/api/v1/notifications/internal/notifications"
                        )
                                .contentType("application/json")
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.created").value(false)
                );

        long notificationCount =
                notificationRepository
                        .findAll()
                        .stream()
                        .filter(notification ->
                                sourceEventId.equals(
                                        notification
                                                .getSourceEventId()
                                )
                        )
                        .count();

        org.assertj.core.api.Assertions
                .assertThat(notificationCount)
                .isEqualTo(1);
    }

    @Test
    void searchShouldFilterByRecipientAndStatus()
            throws Exception {
        UUID recipientUserId = UUID.randomUUID();

        Notification unreadNotification =
                createNotification(recipientUserId);

        Notification readNotification =
                createNotification(recipientUserId);

        readNotification.markAsRead();

        notificationRepository.save(unreadNotification);
        notificationRepository.save(readNotification);

        notificationRepository.saveAndFlush(
                createNotification(UUID.randomUUID())
        );

        mockMvc.perform(
                        get("/api/v1/notifications")
                                .param(
                                        "recipientUserId",
                                        recipientUserId.toString()
                                )
                                .param(
                                        "status",
                                        "UNREAD"
                                )
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content", hasSize(1))
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].status"
                        ).value("UNREAD")
                );
    }

    @Test
    void statusEndpointsShouldReadAndArchiveNotification()
            throws Exception {
        UUID recipientUserId = UUID.randomUUID();

        Notification notification =
                notificationRepository.saveAndFlush(
                        createNotification(
                                recipientUserId
                        )
                );

        mockMvc.perform(
                        post(
                                "/api/v1/notifications/{id}/read",
                                notification.getId()
                        )
                                .param(
                                        "recipientUserId",
                                        recipientUserId.toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("READ")
                )
                .andExpect(
                        jsonPath("$.readAt").exists()
                );

        mockMvc.perform(
                        post(
                                "/api/v1/notifications/{id}/archive",
                                notification.getId()
                        )
                                .param(
                                        "recipientUserId",
                                        recipientUserId.toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("ARCHIVED")
                )
                .andExpect(
                        jsonPath("$.archivedAt").exists()
                );

        mockMvc.perform(
                        post(
                                "/api/v1/notifications/{id}/read",
                                notification.getId()
                        )
                                .param(
                                        "recipientUserId",
                                        recipientUserId.toString()
                                )
                )
                .andExpect(status().isConflict());
    }

    @Test
    void unreadCountShouldReturnRecipientCount()
            throws Exception {
        UUID recipientUserId = UUID.randomUUID();

        notificationRepository.save(
                createNotification(recipientUserId)
        );

        Notification readNotification =
                createNotification(recipientUserId);

        readNotification.markAsRead();

        notificationRepository.saveAndFlush(
                readNotification
        );

        mockMvc.perform(
                        get(
                                "/api/v1/notifications/unread-count"
                        )
                                .param(
                                        "recipientUserId",
                                        recipientUserId.toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.recipientUserId")
                                .value(
                                        recipientUserId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.unreadCount")
                                .value(1)
                );
    }

    @Test
    void createShouldRejectIncompleteReference()
            throws Exception {
        String requestBody = """
                {
                  "recipientUserId": "%s",
                  "type": "GENERAL",
                  "title": "Test notification",
                  "message": "Test message",
                  "referenceType": "ENROLLMENT"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(
                        post(
                                "/api/v1/notifications/internal/notifications"
                        )
                                .contentType("application/json")
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Reference type and reference ID "
                                                + "must be provided together"
                                )
                );
    }

    private Notification createNotification(
            UUID recipientUserId
    ) {
        return Notification.create(
                recipientUserId,
                NotificationType.GENERAL,
                "Test notification",
                "Test notification message.",
                null,
                null,
                UUID.randomUUID()
        );
    }
}