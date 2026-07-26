package com.centerflow.identity.auth.api;

import com.centerflow.identity.auth.application.UserRegistrationResult;
import com.centerflow.identity.auth.application.UserRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class RegistrationController {

    private final UserRegistrationService registrationService;

    public RegistrationController(
            UserRegistrationService registrationService
    ) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterUserResponse register(
            @Valid @RequestBody RegisterUserRequest request
    ) {
        UserRegistrationResult result =
                registrationService.registerStudent(
                        request.email(),
                        request.password()
                );

        return RegisterUserResponse.from(result);
    }
}