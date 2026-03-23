package com.diev.security;

import java.util.UUID;

public record JwtPrincipal(
        UUID id,
        String email,
        String role
) {
}