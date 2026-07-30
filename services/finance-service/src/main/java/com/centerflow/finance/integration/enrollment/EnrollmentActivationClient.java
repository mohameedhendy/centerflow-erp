package com.centerflow.finance.integration.enrollment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class EnrollmentActivationClient {

    private static final String ACTIVATE_PATH =
            "/api/v1/enrollments/internal/{enrollmentId}/activate";

    private final RestClient restClient;
    private final boolean enabled;

    public EnrollmentActivationClient(
            @Value(
                    "${centerflow.integration.enrollment.base-url:http://localhost:8083}"
            )
            String baseUrl,

            @Value(
                    "${centerflow.integration.enrollment.enabled:true}"
            )
            boolean enabled
    ) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Enrollment Service base URL is required"
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

    public void activateEnrollment(UUID enrollmentId) {
        if (!enabled) {
            throw new IllegalStateException(
                    "Enrollment activation integration is disabled"
            );
        }

        restClient
                .post()
                .uri(
                        ACTIVATE_PATH,
                        enrollmentId
                )
                .retrieve()
                .toBodilessEntity();
    }

    public boolean isEnabled() {
        return enabled;
    }
}