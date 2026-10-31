
package com.roshan.devopshub.auth;

import com.roshan.devopshub.security.JwtService;
import com.roshan.devopshub.user.User;
import com.roshan.devopshub.user.UserRepository;
import com.roshan.devopshub.user.UserResponse;
import com.roshan.devopshub.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail =
                request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User(
                normalizedEmail,
                request.displayName().trim(),
                UserRole.VIEWER
        );

        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);

        return UserResponse.from(savedUser);
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        String normalizedEmail =
                request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(
                request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.create(user);

        LoginResponse response = LoginResponse.bearer(
                accessToken,
                jwtService.getExpirationSeconds()
        );

        return new LoginResult(response, refreshToken);
    }
    @Transactional
    public LoginResult refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotation =
                refreshTokenService.rotate(rawRefreshToken);

        String accessToken =
                jwtService.generateAccessToken(rotation.user());

        LoginResponse response = LoginResponse.bearer(
                accessToken,
                jwtService.getExpirationSeconds()
        );

        return new LoginResult(response, rotation.refreshToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }
    public record LoginResult(
            LoginResponse response,
            String refreshToken
    ) {
    }
}
