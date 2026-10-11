package com.roshan.devopshub.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
    public static LoginResponse bearer(String token, long expiresIn) {
        return new LoginResponse(token, "Bearer", expiresIn);
    }
}