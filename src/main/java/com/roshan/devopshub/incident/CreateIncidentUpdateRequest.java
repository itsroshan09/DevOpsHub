
package com.roshan.devopshub.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIncidentUpdateRequest(
        @NotBlank(message = "Update message is required")
        @Size(max = 5000, message = "Update message must not exceed 5000 characters")
        String message
) {
}
