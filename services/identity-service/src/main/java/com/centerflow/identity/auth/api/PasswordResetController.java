package com.centerflow.identity.auth.api;

import com.centerflow.identity.auth.application.PasswordResetRequestResult;
import com.centerflow.identity.auth.application.PasswordResetService;
import com.centerflow.identity.security.password.PasswordResetProperties;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password")
public class PasswordResetController {

    private static final String GENERIC_MESSAGE =
            "If an active account exists for this email, password reset instructions have been created";

    private final PasswordResetService
            passwordResetService;

    private final PasswordResetProperties
            passwordResetProperties;

    public PasswordResetController(
            PasswordResetService passwordResetService,
            PasswordResetProperties passwordResetProperties
    ) {
        this.passwordResetService =
                passwordResetService;

        this.passwordResetProperties =
                passwordResetProperties;
    }

    @PostMapping("/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ForgotPasswordResponse forgotPassword(
            @Valid @RequestBody
            ForgotPasswordRequest request
    ) {
        PasswordResetRequestResult result =
                passwordResetService.requestReset(
                        request.email()
                );

        if (passwordResetProperties
                .exposeTokenInResponse()
                && result.issued()) {

            return new ForgotPasswordResponse(
                    GENERIC_MESSAGE,
                    result.tokenValue(),
                    result.expiresAt()
            );
        }

        return new ForgotPasswordResponse(
                GENERIC_MESSAGE,
                null,
                null
        );
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @Valid @RequestBody
            ResetPasswordRequest request
    ) {
        passwordResetService.resetPassword(
                request.resetToken(),
                request.newPassword()
        );
    }
}