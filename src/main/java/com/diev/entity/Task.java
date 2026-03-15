package com.diev.entity;

import java.util.UUID;

public record Task(
        UUID id,
        String title,
        String description,
        Integer reward,
        TaskStatus status,
        UUID customerId,
        UUID executorId
) {}
