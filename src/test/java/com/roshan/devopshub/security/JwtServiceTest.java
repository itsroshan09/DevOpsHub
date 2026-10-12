package com.roshan.devopshub.security;

import com.roshan.devopshub.user.User;
import com.roshan.devopshub.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET =
            Base64.getEncoder().encodeToString(
                    "0123456789abcdef0123456789abcdef"
                            .getBytes(StandardCharsets.UTF_8));

    private final JwtService jwtService = new JwtService(SECRET, 900);

    @Test
    void shouldParseValidAccessToken() {
        User user = mock(User.class);

        when(user.getId()).thenReturn(42L);
        when(user.getRole()).thenReturn(UserRole.RESPONDER);

        String token = jwtService.generateAccessToken(user);

        Claims claims = jwtService.parseAccessToken(token).getPayload();

        assertEquals("42", claims.getSubject());
        assertEquals("RESPONDER", claims.get("role"));
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void shouldRejectExpiredAccessToken() {
        SecretKey key = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(SECRET));

        Instant now = Instant.now();

        String expiredToken = Jwts.builder()
                .subject("42")
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(key)
                .compact();

        assertThrows(
                ExpiredJwtException.class,
                () -> jwtService.parseAccessToken(expiredToken)
        );
    }

    @Test
    void shouldRejectNonPositiveExpiration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService(SECRET, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService(SECRET, -1)
        );
    }


}
