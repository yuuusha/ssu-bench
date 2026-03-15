package com.diev.entity;

import java.util.UUID;

public record User(
        UUID id,
        String email,
        String password,
        String role,
        Integer balance,
        Boolean blocked
) {}
