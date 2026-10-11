
package com.roshan.devopshub.user;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        UserRole role,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
