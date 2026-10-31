
package com.roshan.devopshub.auth;

import com.roshan.devopshub.user.User;
import com.roshan.devopshub.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private RefreshTokenRepository repository;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenRepository.class);
        service = new RefreshTokenService(repository, 3600);
    }

    @Test
    void createShouldStoreHashRatherThanRawToken() {
        User user = new User(
                "test@example.com",
                "Test User",
                UserRole.VIEWER
        );

        RefreshToken[] saved = new RefreshToken[1];

        when(repository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> {
                    saved[0] = invocation.getArgument(0);
                    return saved[0];
                });

        String rawToken = service.create(user);

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());
        assertNotEquals(rawToken, saved[0].getTokenHash());
        assertEquals(64, saved[0].getTokenHash().length());
        assertEquals(user, saved[0].getUser());
        assertTrue(saved[0].getExpiresAt().isAfter(Instant.now()));

        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void createShouldGenerateDifferentTokens() {
        User user = new User(
                "test@example.com",
                "Test User",
                UserRole.VIEWER
        );

        when(repository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String first = service.create(user);
        String second = service.create(user);

        assertNotEquals(first, second);
    }

    @Test
    void rotateShouldRevokeOldTokenAndCreateReplacement() {
        User user = new User(
                "test@example.com",
                "Test User",
                UserRole.VIEWER
        );

        RefreshToken current = new RefreshToken(
                user,
                "stored-hash",
                Instant.now().plusSeconds(3600)
        );

        when(repository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(current));

        when(repository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.RotationResult result =
                service.rotate("presented-refresh-token");

        assertTrue(current.isRevoked());
        assertNotNull(result.refreshToken());
        assertNotEquals("presented-refresh-token", result.refreshToken());
        assertEquals(user, result.user());

        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void rotateShouldRejectUnknownToken() {
        when(repository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.rotate("unknown-token")
        );

        verify(repository, never()).save(any(RefreshToken.class));
    }

    @Test
    void rotateShouldRejectExpiredToken() {
        User user = new User(
                "test@example.com",
                "Test User",
                UserRole.VIEWER
        );

        RefreshToken expired = new RefreshToken(
                user,
                "expired-hash",
                Instant.now().minusSeconds(60)
        );

        when(repository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(expired));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.rotate("expired-token")
        );

        verify(repository, never()).save(any(RefreshToken.class));
    }

    @Test
    void rotateShouldRejectRevokedToken() {
        User user = new User(
                "test@example.com",
                "Test User",
                UserRole.VIEWER
        );

        RefreshToken revoked = new RefreshToken(
                user,
                "revoked-hash",
                Instant.now().plusSeconds(3600)
        );
        revoked.revoke();

        when(repository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(revoked));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.rotate("revoked-token")
        );

        verify(repository, never()).save(any(RefreshToken.class));
    }

    @Test
    void revokeShouldNotFailWhenTokenIsUnknown() {
        when(repository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.revoke("unknown-token"));

        verify(repository).findByTokenHash(anyString());
    }

    @Test
    void constructorShouldRejectNonPositiveExpiration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RefreshTokenService(repository, 0)
        );
    }
}