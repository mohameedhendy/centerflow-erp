package com.centerflow.identity.auth.api;

import com.centerflow.identity.auth.application.UserAuthenticationService;
import com.centerflow.identity.auth.application.UserLoginResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    private final UserAuthenticationService
            authenticationService;

    public LoginController(
            UserAuthenticationService authenticationService
    ) {
        this.authenticationService =
                authenticationService;
    }

    @PostMapping("/login")
    public LoginUserResponse login(
            @Valid @RequestBody LoginUserRequest request
    ) {
        UserLoginResult result =
                authenticationService.login(
                        request.email(),
                        request.password()
                );

        return LoginUserResponse.from(result);
    }
}