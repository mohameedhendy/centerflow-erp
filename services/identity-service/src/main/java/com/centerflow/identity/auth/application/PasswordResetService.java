package com.centerflow.identity.auth.application;

import com.centerflow.identity.common.exception.InvalidPasswordResetTokenException;
import com.centerflow.identity.security.password.PasswordResetProperties;
import com.centerflow.identity.security.password.PasswordResetToken;
import com.centerflow.identity.security.password.PasswordResetTokenCodec;
import com.centerflow.identity.security.password.PasswordResetTokenRepository;
import com.centerflow.identity.security.session.RefreshTokenSessionRepository;
import com.centerflow.identity.user.domain.User;
import com.centerflow.identity.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;

    private final PasswordResetTokenRepository
            passwordResetTokenRepository;

    private final RefreshTokenSessionRepository
            refreshTokenSessionRepository;

    private final PasswordResetTokenCodec
            passwordResetTokenCodec;

    private final PasswordResetProperties
            passwordResetProperties;

    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository
                    passwordResetTokenRepository,
            RefreshTokenSessionRepository
                    refreshTokenSessionRepository,
            PasswordResetTokenCodec
                    passwordResetTokenCodec,
            PasswordResetProperties
                    passwordResetProperties,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userRepository = userRepository;

        this.passwordResetTokenRepository =
                passwordResetTokenRepository;

        this.refreshTokenSessionRepository =
                refreshTokenSessionRepository;

        this.passwordResetTokenCodec =
                passwordResetTokenCodec;

        this.passwordResetProperties =
                passwordResetProperties;

        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public PasswordResetRequestResult requestReset(
            String email
    ) {
        String normalizedEmail = email
                .strip()
                .toLowerCase(Locale.ROOT);

        Optional<User> optionalUser =
                userRepository.findByEmailIgnoreCase(
                        normalizedEmail
                );

        if (optionalUser.isEmpty()
                || !optionalUser.get().isActive()) {
            return PasswordResetRequestResult
                    .notIssued();
        }

        User user = optionalUser.get();
        Instant requestedAt = Instant.now(clock);

        passwordResetTokenRepository
                .revokeAllActiveByUserId(
                        user.getId(),
                        requestedAt
                );

        String tokenValue =
                passwordResetTokenCodec.generate();

        String tokenHash =
                passwordResetTokenCodec.hash(
                        tokenValue
                );

        Instant expiresAt = requestedAt.plus(
                passwordResetProperties.tokenTtl()
        );

        PasswordResetToken resetToken =
                PasswordResetToken.create(
                        user.getId(),
                        tokenHash,
                        expiresAt,
                        requestedAt
                );

        passwordResetTokenRepository
                .saveAndFlush(resetToken);

        return PasswordResetRequestResult.issued(
                tokenValue,
                expiresAt
        );
    }

    @Transactional
    public void resetPassword(
            String rawResetToken,
            String newPassword
    ) {
        Instant resetAt = Instant.now(clock);

        String tokenHash =
                passwordResetTokenCodec.hash(
                        rawResetToken
                );

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByTokenHashForUpdate(
                                tokenHash
                        )
                        .orElseThrow(
                                InvalidPasswordResetTokenException::new
                        );

        if (!resetToken.isActiveAt(resetAt)) {
            throw new InvalidPasswordResetTokenException();
        }

        User user = userRepository
                .findById(resetToken.getUserId())
                .orElseThrow(
                        InvalidPasswordResetTokenException::new
                );

        if (!user.isActive()) {
            throw new InvalidPasswordResetTokenException();
        }

        String newPasswordHash =
                passwordEncoder.encode(newPassword);

        user.changePasswordHash(
                newPasswordHash,
                resetAt
        );

        resetToken.markUsed(resetAt);

        passwordResetTokenRepository
                .saveAndFlush(resetToken);

        passwordResetTokenRepository
                .revokeAllActiveByUserId(
                        user.getId(),
                        resetAt
                );

        refreshTokenSessionRepository
                .revokeAllActiveByUserId(
                        user.getId(),
                        resetAt
                );
    }
}