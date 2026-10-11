package com.roshan.devopshub.service;

import java.time.Instant;

public record ServiceResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}