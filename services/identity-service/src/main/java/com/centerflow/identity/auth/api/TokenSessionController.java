package com.centerflow.identity.auth.api;

import com.centerflow.identity.auth.application.UserLoginResult;
import com.centerflow.identity.auth.application.UserTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class TokenSessionController {

    private final UserTokenService userTokenService;

    public TokenSessionController(
            UserTokenService userTokenService
    ) {
        this.userTokenService = userTokenService;
    }

    @PostMapping("/refresh")
    public LoginUserResponse refresh(
            @Valid @RequestBody
            RefreshTokenRequest request
    ) {
        UserLoginResult result =
                userTokenService.refresh(
                        request.refreshToken()
                );

        return LoginUserResponse.from(result);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @Valid @RequestBody
            RefreshTokenRequest request
    ) {
        userTokenService.logout(
                request.refreshToken()
        );
    }
}