package com.centerflow.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record GatewayJwtProperties(
        String issuer,
        String audience,
        String secret
) {

    public GatewayJwtProperties {
        issuer = requireText(
                issuer,
                "JWT issuer is required"
        );

        audience = requireText(
                audience,
                "JWT audience is required"
        );

        secret = requireText(
                secret,
                "JWT secret is required"
        );

        if (secret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must contain at least 32 characters"
            );
        }
    }

    private static String requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.strip();
    }
}