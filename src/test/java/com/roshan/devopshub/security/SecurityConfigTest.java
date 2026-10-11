
package com.roshan.devopshub.security;

import com.roshan.devopshub.auth.AuthController;
import com.roshan.devopshub.auth.AuthService;
import com.roshan.devopshub.auth.LoginRequest;
import com.roshan.devopshub.auth.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @Test
    void protectedEndpointShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(jwtService);
    }

    @Test
    void protectedEndpointShouldReturn401ForMalformedToken()
            throws Exception {
        when(jwtService.parseAccessToken("not-a-valid-token"))
                .thenThrow(new io.jsonwebtoken.JwtException("Invalid token"));

        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginEndpointShouldBePublic() throws Exception {
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "Strong-Test-Password-123!"
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(LoginResponse.bearer("test-token", 900));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).login(any(LoginRequest.class));
    }
}
