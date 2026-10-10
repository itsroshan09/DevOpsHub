
package com.roshan.devopshub.incident;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignIncidentRequest(
        @NotNull(message = "User ID is required")
        @Positive(message = "User ID must be positive")
        Long userId
) {
}
