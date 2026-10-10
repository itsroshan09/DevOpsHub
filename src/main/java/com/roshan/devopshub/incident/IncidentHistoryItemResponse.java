package com.roshan.devopshub.incident;

import java.time.Instant;

public record IncidentHistoryItemResponse(
        String type,
        String details,
        Instant createdAt
) {
}