
package com.roshan.devopshub.auth;

import com.roshan.devopshub.user.User;
import com.roshan.devopshub.user.UserRepository;
import com.roshan.devopshub.user.UserResponse;
import com.roshan.devopshub.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.roshan.devopshub.security.JwtService;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail =
                request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(
                request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateAccessToken(user);

        return LoginResponse.bearer(
                token,
                jwtService.getExpirationSeconds()
        );
    }
}
