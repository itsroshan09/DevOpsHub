
package com.roshan.devopshub.auth;

import com.roshan.devopshub.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long expirationSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.auth.refresh-token-expiration-seconds:2592000}")
            long expirationSeconds
    ) {
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Refresh-token expiration must be positive"
            );
        }

        this.refreshTokenRepository = refreshTokenRepository;
        this.expirationSeconds = expirationSeconds;
    }

    @Transactional
    public String create(User user) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        RefreshToken token = new RefreshToken(
                user,
                hash(rawToken),
                Instant.now().plusSeconds(expirationSeconds)
        );

        refreshTokenRepository.save(token);

        return rawToken;
    }


    @Transactional
    public RotationResult rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            System.out.println(
                    "Refresh diagnostic: cookie token is missing or blank"
            );
            throw new InvalidRefreshTokenException();
        }

        String tokenHash = hash(rawToken);

        var optionalToken =
                refreshTokenRepository.findByTokenHashForUpdate(tokenHash);

        if (optionalToken.isEmpty()) {
            System.out.println(
                    "Refresh diagnostic: token hash did not match a database record"
            );
            throw new InvalidRefreshTokenException();
        }

        RefreshToken current = optionalToken.get();

        if (current.isExpired()) {
            System.out.println("Refresh diagnostic: matching token is expired");
            throw new InvalidRefreshTokenException();
        }

        if (current.isRevoked()) {
            System.out.println("Refresh diagnostic: matching token is revoked");
            throw new InvalidRefreshTokenException();
        }

        User user = current.getUser();
        current.revoke();

        String replacement = create(user);

        System.out.println(
                "Refresh diagnostic: token accepted and rotation completed"
        );

        return new RotationResult(user, replacement);
    }


    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(RefreshToken::revoke);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable", exception
            );
        }
    }

    public record RotationResult(User user, String refreshToken) {
    }
}
