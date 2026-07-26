package com.centerflow.identity.auth.application;

import com.centerflow.identity.common.exception.InvalidCredentialsException;
import com.centerflow.identity.security.jwt.AccessTokenResult;
import com.centerflow.identity.security.jwt.JwtAccessTokenService;
import com.centerflow.identity.security.user.IdentityUserPrincipal;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserAuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtAccessTokenService accessTokenService;

    public UserAuthenticationService(
            AuthenticationManager authenticationManager,
            JwtAccessTokenService accessTokenService
    ) {
        this.authenticationManager =
                authenticationManager;
        this.accessTokenService =
                accessTokenService;
    }

    public UserLoginResult login(
            String email,
            String rawPassword
    ) {
        String normalizedEmail = email
                .strip()
                .toLowerCase(Locale.ROOT);

        Authentication authentication;

        try {
            authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    normalizedEmail,
                                    rawPassword
                            )
                    );
        } catch (
                BadCredentialsException
                | AccountStatusException exception
        ) {
            throw new InvalidCredentialsException();
        }

        IdentityUserPrincipal principal =
                (IdentityUserPrincipal)
                        authentication.getPrincipal();

        AccessTokenResult accessToken =
                accessTokenService.issueToken(principal);

        return new UserLoginResult(
                accessToken.tokenValue(),
                "Bearer",
                accessToken.expiresInSeconds(),
                accessToken.expiresAt(),
                principal.getUserId(),
                principal.getUsername(),
                principal.getRoles()
        );
    }
}