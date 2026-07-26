package com.centerflow.identity.security.password;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "security.password-reset"
)
public record PasswordResetProperties(
        Duration tokenTtl,
        boolean exposeTokenInResponse
) {

    public PasswordResetProperties {
        if (tokenTtl == null
                || tokenTtl.isZero()
                || tokenTtl.isNegative()) {

            throw new IllegalArgumentException(
                    "Password reset token TTL must be positive"
            );
        }
    }
}