package com.centerflow.identity.auth.application;

import com.centerflow.identity.common.exception.InvalidRefreshTokenException;
import com.centerflow.identity.security.config.JwtProperties;
import com.centerflow.identity.security.jwt.AccessTokenResult;
import com.centerflow.identity.security.jwt.JwtAccessTokenService;
import com.centerflow.identity.security.session.RefreshTokenCodec;
import com.centerflow.identity.security.session.RefreshTokenSession;
import com.centerflow.identity.security.session.RefreshTokenSessionRepository;
import com.centerflow.identity.security.user.IdentityUserDetailsService;
import com.centerflow.identity.security.user.IdentityUserPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class UserTokenService {

    private final RefreshTokenSessionRepository
            refreshTokenSessionRepository;

    private final RefreshTokenCodec refreshTokenCodec;
    private final JwtAccessTokenService accessTokenService;
    private final IdentityUserDetailsService userDetailsService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public UserTokenService(
            RefreshTokenSessionRepository
                    refreshTokenSessionRepository,
            RefreshTokenCodec refreshTokenCodec,
            JwtAccessTokenService accessTokenService,
            IdentityUserDetailsService userDetailsService,
            JwtProperties jwtProperties,
            Clock clock
    ) {
        this.refreshTokenSessionRepository =
                refreshTokenSessionRepository;

        this.refreshTokenCodec =
                refreshTokenCodec;

        this.accessTokenService =
                accessTokenService;

        this.userDetailsService =
                userDetailsService;

        this.jwtProperties =
                jwtProperties;

        this.clock = clock;
    }

    @Transactional
    public UserLoginResult issueFor(
            IdentityUserPrincipal principal
    ) {
        return issueTokenPair(
                principal,
                Instant.now(clock)
        );
    }

    @Transactional
    public UserLoginResult refresh(
            String rawRefreshToken
    ) {
        Instant refreshedAt = Instant.now(clock);

        String tokenHash =
                refreshTokenCodec.hash(
                        rawRefreshToken
                );

        RefreshTokenSession currentSession =
                refreshTokenSessionRepository
                        .findByTokenHashForUpdate(
                                tokenHash
                        )
                        .orElseThrow(
                                InvalidRefreshTokenException::new
                        );

        if (!currentSession.isActiveAt(refreshedAt)) {
            throw new InvalidRefreshTokenException();
        }

        IdentityUserPrincipal principal;

        try {
            principal = userDetailsService.loadUserById(
                    currentSession.getUserId()
            );
        } catch (
                UsernameNotFoundException exception
        ) {
            throw new InvalidRefreshTokenException(
                    exception
            );
        }

        if (!principal.isEnabled()
                || !principal.isAccountNonLocked()) {
            throw new InvalidRefreshTokenException();
        }

        currentSession.markUsedAndRevoke(
                refreshedAt
        );

        return issueTokenPair(
                principal,
                refreshedAt
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash =
                refreshTokenCodec.hash(
                        rawRefreshToken
                );

        Instant revokedAt = Instant.now(clock);

        refreshTokenSessionRepository
                .findByTokenHashForUpdate(tokenHash)
                .ifPresent(session ->
                        session.revoke(revokedAt)
                );
    }

    private UserLoginResult issueTokenPair(
            IdentityUserPrincipal principal,
            Instant issuedAt
    ) {
        AccessTokenResult accessToken =
                accessTokenService.issueToken(
                        principal
                );

        IssuedRefreshToken refreshToken =
                createRefreshTokenSession(
                        principal,
                        issuedAt
                );

        return new UserLoginResult(
                accessToken.tokenValue(),
                "Bearer",
                accessToken.expiresInSeconds(),
                accessToken.expiresAt(),
                refreshToken.tokenValue(),
                refreshToken.expiresInSeconds(),
                refreshToken.expiresAt(),
                principal.getUserId(),
                principal.getUsername(),
                principal.getRoles()
        );
    }

    private IssuedRefreshToken
    createRefreshTokenSession(
            IdentityUserPrincipal principal,
            Instant issuedAt
    ) {
        String tokenValue =
                refreshTokenCodec.generate();

        String tokenHash =
                refreshTokenCodec.hash(
                        tokenValue
                );

        Instant expiresAt = issuedAt.plus(
                jwtProperties.refreshTokenTtl()
        );

        RefreshTokenSession session =
                RefreshTokenSession.create(
                        principal.getUserId(),
                        tokenHash,
                        expiresAt,
                        issuedAt
                );

        refreshTokenSessionRepository
                .saveAndFlush(session);

        return new IssuedRefreshToken(
                tokenValue,
                issuedAt,
                expiresAt
        );
    }

    private record IssuedRefreshToken(
            String tokenValue,
            Instant issuedAt,
            Instant expiresAt
    ) {

        private long expiresInSeconds() {
            return expiresAt.getEpochSecond()
                    - issuedAt.getEpochSecond();
        }
    }
}