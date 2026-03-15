package com.diev.entity;

import java.util.UUID;

public record Bid(
        UUID id,
        Long taskId,
        Long executorId,
        String status
) {}