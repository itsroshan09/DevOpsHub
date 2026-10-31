package com.roshan.devopshub.auth;

import com.roshan.devopshub.common.GlobalExceptionHandler;
import com.roshan.devopshub.user.UserResponse;
import com.roshan.devopshub.user.UserRole;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Instant;
import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@ImportAutoConfiguration(
        exclude = UserDetailsServiceAutoConfiguration.class
)
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @Test
    void registerShouldReturn201AndNotExposePasswordHash() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "Test User",
                "Strong-Test-Password-123!"
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new UserResponse(
                        1L,
                        "test@example.com",
                        "Test User",
                        UserRole.VIEWER,
                        Instant.parse("2026-01-01T00:00:00Z")
                ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void registerShouldReturn400WhenEmailIsInvalid() throws Exception {
        String requestBody = """
                {
                    "email": "not-an-email",
                    "displayName": "Test User",
                    "password": "Strong-Test-Password-123!"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void registerShouldReturn400WhenPasswordIsTooShort() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "displayName": "Test User",
                    "password": "short"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void loginShouldReturnAccessTokenAndSetHttpOnlyRefreshCookie()
            throws Exception {

        LoginRequest request = new LoginRequest(
                "test@example.com",
                "Strong-Test-Password-123!"
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthService.LoginResult(
                        LoginResponse.bearer("test-access-token", 900),
                        "test-refresh-token"
                ));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("test-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        String responseBody = result.getResponse().getContentAsString();

        assertTrue(setCookie.contains("devopshub_refresh_token="));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Strict"));
        assertTrue(setCookie.contains("Path=/api/auth"));

        assertFalse(responseBody.contains("test-refresh-token"));

        verify(authService).login(any(LoginRequest.class));
    }
    @Test
    void refreshShouldReturnNewAccessTokenAndRotateRefreshCookie()
            throws Exception {

        when(authService.refresh("old-refresh-token"))
                .thenReturn(new AuthService.LoginResult(
                        LoginResponse.bearer("new-access-token", 900),
                        "new-refresh-token"
                ));

        MvcResult result = mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(new Cookie(
                                        "devopshub_refresh_token",
                                        "old-refresh-token"
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("new-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");

        assertTrue(setCookie.contains("devopshub_refresh_token="));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Strict"));
        assertTrue(setCookie.contains("Path=/api/auth"));

        assertFalse(result.getResponse().getContentAsString()
                .contains("new-refresh-token"));

        verify(authService).refresh("old-refresh-token");
    }
    @Test
    void logoutShouldRevokeRefreshTokenAndClearCookie()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/logout")
                                .cookie(new Cookie(
                                        "devopshub_refresh_token",
                                        "test-refresh-token"
                                ))
                )
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"));

        verify(authService).logout("test-refresh-token");
    }
    @Test
    void refreshShouldClearCookieWhenRefreshTokenIsInvalid()
            throws Exception {

        when(authService.refresh("invalid-refresh-token"))
                .thenThrow(new InvalidRefreshTokenException());

        MvcResult result = mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(new Cookie(
                                        "devopshub_refresh_token",
                                        "invalid-refresh-token"
                                ))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");

        assertTrue(setCookie.contains("Max-Age=0"));
        assertTrue(setCookie.contains("HttpOnly"));

        verify(authService).refresh("invalid-refresh-token");
    }
}
