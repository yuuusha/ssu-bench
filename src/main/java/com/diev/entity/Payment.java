package com.diev.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public record Payment(
        UUID id,
        Long fromUser,
        Long toUser,
        Integer amount,
        LocalDateTime createdAt
) {}