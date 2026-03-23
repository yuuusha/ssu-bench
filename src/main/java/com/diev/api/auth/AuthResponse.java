package com.diev.api.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        AuthUserResponse user
) {
}