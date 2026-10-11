package com.roshan.devopshub.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceCreateRequest(

        @NotBlank(message = "Service name is required")
        @Size(max = 150, message = "Service name must not exceed 150 characters")
        String name,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description

) {
}