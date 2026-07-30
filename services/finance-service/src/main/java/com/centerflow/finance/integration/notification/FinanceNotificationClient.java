package com.centerflow.finance.integration.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FinanceNotificationClient {

    private static final String CREATE_NOTIFICATION_PATH =
            "/api/v1/notifications/internal/notifications";

    private final RestClient restClient;
    private final boolean enabled;

    public FinanceNotificationClient(
            @Value(
                    "${centerflow.integration.notification.base-url:http://localhost:8085}"
            )
            String baseUrl,

            @Value(
                    "${centerflow.integration.notification.enabled:true}"
            )
            boolean enabled
    ) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Notification Service base URL is required"
            );
        }

        String normalizedBaseUrl = baseUrl.trim();

        if (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(
                    0,
                    normalizedBaseUrl.length() - 1
            );
        }

        this.restClient = RestClient
                .builder()
                .baseUrl(normalizedBaseUrl)
                .build();

        this.enabled = enabled;
    }

    public void createNotification(
            FinanceNotificationEvent event
    ) {
        if (!enabled) {
            return;
        }

        restClient
                .post()
                .uri(CREATE_NOTIFICATION_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        FinanceNotificationRequest.from(
                                event
                        )
                )
                .retrieve()
                .toBodilessEntity();
    }

    public boolean isEnabled() {
        return enabled;
    }
}