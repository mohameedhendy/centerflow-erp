package com.centerflow.identity.authorization.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "security.bootstrap-admin"
)
public record AdminBootstrapProperties(
        boolean enabled,
        String email,
        String password
) {

    public AdminBootstrapProperties {
        if (enabled) {
            email = requireText(
                    email,
                    "Bootstrap administrator email is required"
            );

            password = requireText(
                    password,
                    "Bootstrap administrator password is required"
            );

            if (!email.contains("@")
                    || email.startsWith("@")
                    || email.endsWith("@")) {
                throw new IllegalArgumentException(
                        "Bootstrap administrator email format is invalid"
                );
            }

            if (email.length() > 320) {
                throw new IllegalArgumentException(
                        "Bootstrap administrator email must not exceed 320 characters"
                );
            }

            if (password.length() < 12
                    || password.length() > 64) {
                throw new IllegalArgumentException(
                        "Bootstrap administrator password must be between 12 and 64 characters"
                );
            }

            boolean containsLetter =
                    password.codePoints()
                            .anyMatch(
                                    Character::isLetter
                            );

            boolean containsDigit =
                    password.codePoints()
                            .anyMatch(
                                    Character::isDigit
                            );

            if (!containsLetter || !containsDigit) {
                throw new IllegalArgumentException(
                        "Bootstrap administrator password must contain at least one letter and one number"
                );
            }
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