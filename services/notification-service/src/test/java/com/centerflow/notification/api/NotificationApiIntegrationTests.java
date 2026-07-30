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

    private static final String USER_ID_HEADER =
            "X-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository
            notificationRepository;

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
                                .contentType(
                                        "application/json"
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.created")
                                .value(true)
                );

        mockMvc.perform(
                        post(
                                "/api/v1/notifications/internal/notifications"
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.created")
                                .value(false)
                );
    }

    @Test
    void searchShouldUseAuthenticatedUserHeader()
            throws Exception {
        UUID recipientUserId = UUID.randomUUID();

        notificationRepository.saveAndFlush(
                createNotification(recipientUserId)
        );

        notificationRepository.saveAndFlush(
                createNotification(UUID.randomUUID())
        );

        mockMvc.perform(
                        get("/api/v1/notifications")
                                .header(
                                        USER_ID_HEADER,
                                        recipientUserId
                                                .toString()
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
                        jsonPath(
                                "$.content",
                                hasSize(1)
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].recipientUserId"
                        ).value(
                                recipientUserId.toString()
                        )
                );
    }

    @Test
    void userShouldNotAccessAnotherUsersNotification()
            throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();

        Notification notification =
                notificationRepository.saveAndFlush(
                        createNotification(ownerUserId)
                );

        mockMvc.perform(
                        get(
                                "/api/v1/notifications/{id}",
                                notification.getId()
                        )
                                .header(
                                        USER_ID_HEADER,
                                        anotherUserId.toString()
                                )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void statusEndpointsShouldUseAuthenticatedUser()
            throws Exception {
        UUID recipientUserId = UUID.randomUUID();

        Notification notification =
                notificationRepository.saveAndFlush(
                        createNotification(recipientUserId)
                );

        mockMvc.perform(
                        post(
                                "/api/v1/notifications/{id}/read",
                                notification.getId()
                        )
                                .header(
                                        USER_ID_HEADER,
                                        recipientUserId
                                                .toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("READ")
                );

        mockMvc.perform(
                        post(
                                "/api/v1/notifications/{id}/archive",
                                notification.getId()
                        )
                                .header(
                                        USER_ID_HEADER,
                                        recipientUserId
                                                .toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("ARCHIVED")
                );
    }

    @Test
    void unreadCountShouldUseAuthenticatedUser()
            throws Exception {
        UUID recipientUserId = UUID.randomUUID();

        notificationRepository.saveAndFlush(
                createNotification(recipientUserId)
        );

        mockMvc.perform(
                        get(
                                "/api/v1/notifications/unread-count"
                        )
                                .header(
                                        USER_ID_HEADER,
                                        recipientUserId
                                                .toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.recipientUserId")
                                .value(
                                        recipientUserId
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.unreadCount")
                                .value(1)
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