
package com.roshan.devopshub.incident;

import java.time.Instant;

public record IncidentUpdateResponse(
        Long id,
        Long incidentId,
        String message,
        Instant createdAt
) {
}
