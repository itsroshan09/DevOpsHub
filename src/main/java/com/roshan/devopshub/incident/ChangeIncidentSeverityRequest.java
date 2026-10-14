package com.roshan.devopshub.incident;

import jakarta.validation.constraints.NotNull;

public record ChangeIncidentSeverityRequest(
        @NotNull(message = "Severity is required")
        IncidentSeverity severity
) {
}