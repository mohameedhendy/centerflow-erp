package com.centerflow.identity.auth.application;

import com.centerflow.identity.common.exception.InvalidCredentialsException;
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

    private final AuthenticationManager
            authenticationManager;

    private final UserTokenService userTokenService;

    public UserAuthenticationService(
            AuthenticationManager authenticationManager,
            UserTokenService userTokenService
    ) {
        this.authenticationManager =
                authenticationManager;

        this.userTokenService =
                userTokenService;
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

        return userTokenService.issueFor(principal);
    }
}