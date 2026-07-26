package com.centerflow.identity.security.session;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

@Component
public class RefreshTokenCodec {

    private static final int TOKEN_BYTE_LENGTH = 64;
    private static final String HASH_ALGORITHM =
            "SHA-256";

    private final SecureRandom secureRandom =
            new SecureRandom();

    public String generate() {
        byte[] tokenBytes =
                new byte[TOKEN_BYTE_LENGTH];

        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    public String hash(String rawToken) {
        Objects.requireNonNull(
                rawToken,
                "Refresh token is required"
        );

        if (rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Refresh token is required"
            );
        }

        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            HASH_ALGORITHM
                    );

            byte[] tokenHash = digest.digest(
                    rawToken.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(tokenHash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 hashing is unavailable",
                    exception
            );
        }
    }
}