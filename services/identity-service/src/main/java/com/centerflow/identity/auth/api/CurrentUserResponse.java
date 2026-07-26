package com.centerflow.identity.auth.api;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String email,
        List<String> roles
) {
}