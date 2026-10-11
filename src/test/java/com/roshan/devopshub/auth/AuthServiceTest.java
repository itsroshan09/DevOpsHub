
package com.roshan.devopshub.auth;

import com.roshan.devopshub.user.User;
import com.roshan.devopshub.user.UserRepository;
import com.roshan.devopshub.user.UserResponse;
import com.roshan.devopshub.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.roshan.devopshub.security.JwtService;

import java.time.Instant;
import java.util.Optional;
import java.util.Locale;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = mock(JwtService.class);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void registerShouldHashPasswordAndAssignViewerRole() {
        RegisterRequest request = new RegisterRequest(
                "  TEST@example.com ",
                " Test User ",
                "Strong-Test-Password-123!"
        );

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals("test@example.com", user.getEmail());
            assertEquals("Test User", user.getDisplayName());
            assertEquals(UserRole.VIEWER, user.getRole());
            assertNotEquals(request.password(), user.getPasswordHash());
            assertTrue(passwordEncoder.matches(
                    request.password(), user.getPasswordHash()));

            return user;
        });

        UserResponse response = authService.register(request);

        assertEquals("test@example.com", response.email());
        assertEquals("Test User", response.displayName());
        assertEquals(UserRole.VIEWER, response.role());
        assertNull(response.id()); // Mock repository does not generate IDs.
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "Test User",
                "Strong-Test-Password-123!"
        );

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request)
        );

        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void loginShouldReturnAccessTokenForValidCredentials() {
        String rawPassword = "Strong-Test-Password-123!";
        User user = new User(
                "test@example.com",
                "Test User",
                UserRole.VIEWER
        );
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user))
                .thenReturn("signed-test-token");
        when(jwtService.getExpirationSeconds()).thenReturn(900L);

        LoginResponse response = authService.login(
                new LoginRequest(" TEST@example.com ", rawPassword)
        );

        assertEquals("signed-test-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());

        verify(jwtService).generateAccessToken(user);
    }

    @Test
    void loginShouldRejectUnknownEmail() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(
                        new LoginRequest(
                                "unknown@example.com",
                                "Strong-Test-Password-123!"
                        )
                )
        );

        verify(jwtService, never()).generateAccessToken(any(User.class));
    }

    @Test
    void loginShouldRejectIncorrectPassword() {
        User user = new User(
                "test@example.com",
                "Test User",
                UserRole.VIEWER
        );
        user.setPasswordHash(
                passwordEncoder.encode("Correct-Password-123!")
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(
                        new LoginRequest(
                                "test@example.com",
                                "Wrong-Password-123!"
                        )
                )
        );

        verify(jwtService, never()).generateAccessToken(any(User.class));
    }

    @Test
    void loginShouldRejectUserWithoutPasswordHash() {
        User user = new User(
                "test@example.com",
                "Test User",
                UserRole.VIEWER
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(
                        new LoginRequest(
                                "test@example.com",
                                "Strong-Test-Password-123!"
                        )
                )
        );

        verify(jwtService, never()).generateAccessToken(any(User.class));
    }

}
