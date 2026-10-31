
package com.roshan.devopshub.auth;

import com.roshan.devopshub.user.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import com.roshan.devopshub.auth.InvalidRefreshTokenException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME =
            "devopshub_refresh_token";

    private final AuthService authService;
    private final long refreshTokenExpirationSeconds;
    private final boolean refreshCookieSecure;

    public AuthController(
            AuthService authService,
            @Value("${app.auth.refresh-token-expiration-seconds:2592000}")
            long refreshTokenExpirationSeconds,
            @Value("${app.auth.refresh-cookie-secure:false}")
            boolean refreshCookieSecure
    ) {
        this.authService = authService;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
        this.refreshCookieSecure = refreshCookieSecure;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse servletResponse
    ) {
        AuthService.LoginResult result = authService.login(request);

        setRefreshCookie(
                servletResponse,
                result.refreshToken(),
                refreshTokenExpirationSeconds
        );

        return result.response();
    }
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of(
                "headerName", csrfToken.getHeaderName(),
                "token", csrfToken.getToken()
        );
    }
    private void setRefreshCookie(
            HttpServletResponse response,
            String token,
            long maxAgeSeconds
    ) {
        ResponseCookie cookie = ResponseCookie
                .from(REFRESH_COOKIE_NAME, token == null ? "" : token)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
    @PostMapping("/refresh")
    public LoginResponse refresh(
            @CookieValue(
                    name = REFRESH_COOKIE_NAME,
                    required = false
            ) String refreshToken,
            HttpServletResponse servletResponse
    ) {
        try {
            AuthService.LoginResult result =
                    authService.refresh(refreshToken);

            setRefreshCookie(
                    servletResponse,
                    result.refreshToken(),
                    refreshTokenExpirationSeconds
            );

            return result.response();
        } catch (InvalidRefreshTokenException exception) {
            setRefreshCookie(servletResponse, null, 0);
            throw exception;
        }
    }
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(
                    name = REFRESH_COOKIE_NAME,
                    required = false
            ) String refreshToken,
            HttpServletResponse servletResponse
    ) {
        authService.logout(refreshToken);
        setRefreshCookie(servletResponse, null, 0);
    }
}
