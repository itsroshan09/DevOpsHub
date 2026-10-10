package com.roshan.devopshub.incident;

import jakarta.validation.constraints.NotNull;

public record TransitionIncidentRequest(
        @NotNull(message = "Status is required")
        IncidentStatus status
) {
}