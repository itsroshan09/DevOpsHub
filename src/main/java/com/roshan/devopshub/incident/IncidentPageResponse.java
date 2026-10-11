package com.roshan.devopshub.incident;

import org.springframework.data.domain.Page;

import java.util.List;

public record IncidentPageResponse(
        List<IncidentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static IncidentPageResponse from(
            Page<IncidentResponse> result
    ) {
        return new IncidentPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}