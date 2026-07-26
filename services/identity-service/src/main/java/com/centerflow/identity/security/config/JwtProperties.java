package com.centerflow.identity.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String issuer,
        String audience,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        String secret
) {

    public JwtProperties {
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

        requirePositiveDuration(
                accessTokenTtl,
                "JWT access token TTL must be positive"
        );

        requirePositiveDuration(
                refreshTokenTtl,
                "JWT refresh token TTL must be positive"
        );
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

    private static void requirePositiveDuration(
            Duration duration,
            String message
    ) {
        if (duration == null
                || duration.isZero()
                || duration.isNegative()) {
            throw new IllegalArgumentException(message);
        }
    }
}