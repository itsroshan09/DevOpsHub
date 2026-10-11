package com.roshan.devopshub.security;

import com.roshan.devopshub.auth.AuthService;
import com.roshan.devopshub.auth.LoginRequest;
import com.roshan.devopshub.auth.LoginResponse;
import com.roshan.devopshub.user.User;
import com.roshan.devopshub.user.UserRepository;
import com.roshan.devopshub.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityConfigTest.RoleProbeConfiguration.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

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


    @Test
    void protectedEndpointShouldAcceptValidToken() throws Exception {
        String token = "valid-test-token";

        io.jsonwebtoken.Claims claims =
                io.jsonwebtoken.Jwts.claims()
                        .subject("1")
                        .add("role", "RESPONDER")
                        .build();

        io.jsonwebtoken.Jws<io.jsonwebtoken.Claims> parsedToken =
                org.mockito.Mockito.mock(io.jsonwebtoken.Jws.class);

        org.mockito.Mockito.when(parsedToken.getPayload())
                .thenReturn(claims);

        org.mockito.Mockito.when(jwtService.parseAccessToken(token))
                .thenReturn(parsedToken);
        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(user.getRole()).thenReturn(UserRole.RESPONDER);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointShouldRejectTokenForNonexistentUser()
            throws Exception {

        String token = "valid-token-for-deleted-user";

        Claims claims = Jwts.claims()
                .subject("999")
                .add("role", "ADMIN")
                .build();

        @SuppressWarnings("unchecked")
        Jws<Claims> parsedToken = mock(Jws.class);

        when(parsedToken.getPayload()).thenReturn(claims);
        when(jwtService.parseAccessToken(token)).thenReturn(parsedToken);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        verify(userRepository).findById(999L);
    }

    @TestConfiguration
    static class RoleProbeConfiguration {

        @Bean
        RoleProbeController roleProbeController() {
            return new RoleProbeController();
        }
    }

    @RestController
    static class RoleProbeController {

        @GetMapping("/test/security/current-role")
        public String currentRole() {
            return SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getAuthorities()
                    .iterator()
                    .next()
                    .getAuthority();
        }
    }


    @Test
    void authenticatedRoleShouldComeFromDatabaseNotJwtClaim()
            throws Exception {

        String token = "admin-claim-test-token";

        Claims claims = Jwts.claims()
                .subject("1")
                .add("role", "ADMIN")
                .build();

        @SuppressWarnings("unchecked")
        Jws<Claims> parsedToken = mock(Jws.class);

        when(parsedToken.getPayload()).thenReturn(claims);
        when(jwtService.parseAccessToken(token)).thenReturn(parsedToken);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getRole()).thenReturn(UserRole.VIEWER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/test/security/current-role")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string("ROLE_VIEWER"));
    }



}
