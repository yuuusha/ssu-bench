package com.diev.api.auth;

import com.diev.entity.Role;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        Role role,
        Integer balance,
        Boolean blocked
) {
}