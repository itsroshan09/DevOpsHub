
package com.roshan.devopshub.common;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        List<String> details
) {
}
