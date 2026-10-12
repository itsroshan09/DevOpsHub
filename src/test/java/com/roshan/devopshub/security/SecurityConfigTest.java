
package com.roshan.devopshub.security;

import com.roshan.devopshub.auth.AuthService;
import com.roshan.devopshub.auth.LoginRequest;
import com.roshan.devopshub.auth.LoginResponse;
import com.roshan.devopshub.incident.Incident;
import com.roshan.devopshub.incident.IncidentService;
import com.roshan.devopshub.incident.IncidentSeverity;
import com.roshan.devopshub.user.User;
import com.roshan.devopshub.user.UserRepository;
import com.roshan.devopshub.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.roshan.devopshub.service.ServiceManagementService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;


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

    @MockitoBean
    private IncidentService incidentService;

    @MockitoBean
    private ServiceManagementService serviceManagementService;

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
        mockAuthenticatedUser(1L, UserRole.RESPONDER, "valid-test-token");

        stubEmptyIncidentPage();

        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", "Bearer valid-test-token"))
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
                .andExpect(
                        org.springframework.test.web.servlet.result
                                .MockMvcResultMatchers.content()
                                .string("ROLE_VIEWER")
                );
    }

    @Test
    void viewerShouldReceive403WhenCreatingIncident() throws Exception {
        mockAuthenticatedUser(10L, UserRole.VIEWER, "test-token-10");

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer test-token-10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test incident",
                                  "description": "Created by authorization test",
                                  "severity": "LOW"
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(incidentService);
    }

    @Test
    void responderShouldPassAuthorizationWhenCreatingIncident()
            throws Exception {

        mockAuthenticatedUser(11L, UserRole.RESPONDER, "test-token-11");

        String title = "Test incident";
        String description = "Created by authorization test";
        IncidentSeverity severity = IncidentSeverity.LOW;

        Incident incident = new Incident(title, description, severity);

        when(incidentService.createIncident(11L, title, description, severity))
                .thenReturn(incident);

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer test-token-11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test incident",
                                  "description": "Created by authorization test",
                                  "severity": "LOW"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(incidentService).createIncident(11L, title, description, severity);
    }

    @Test
    void viewerShouldBeAllowedToListIncidents() throws Exception {
        mockAuthenticatedUser(12L, UserRole.VIEWER, "test-token-12");

        stubEmptyIncidentPage();

        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", "Bearer test-token-12"))
                .andExpect(status().isOk());

        verify(incidentService).getIncidents(
                any(),
                any(),
                any(Pageable.class)
        );
    }

    private void mockAuthenticatedUser(
            Long userId,
            UserRole databaseRole,
            String token
    ) {
        Claims claims = Jwts.claims()
                .subject(userId.toString())
                .add("role", "ADMIN")
                .build();

        @SuppressWarnings("unchecked")
        Jws<Claims> parsedToken = mock(Jws.class);

        when(parsedToken.getPayload()).thenReturn(claims);
        when(jwtService.parseAccessToken(token)).thenReturn(parsedToken);

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getRole()).thenReturn(databaseRole);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    private void stubEmptyIncidentPage() {
        when(incidentService.getIncidents(
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));
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
    void viewerShouldReceive403WhenAssigningIncident() throws Exception {
        mockAuthenticatedUser(20L, UserRole.VIEWER, "viewer-assign-token");

        mockMvc.perform(put("/api/incidents/1/assignee")
                        .header(
                                "Authorization",
                                "Bearer viewer-assign-token"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "userId": 21
                            }
                            """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(incidentService);
    }

    @Test
    void viewerShouldReceive403WhenChangingIncidentStatus()
            throws Exception {
        mockAuthenticatedUser(22L, UserRole.VIEWER, "viewer-transition-token");

        mockMvc.perform(post("/api/incidents/1/transitions")
                        .header(
                                "Authorization",
                                "Bearer viewer-transition-token"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "status": "IN_PROGRESS"
                            }
                            """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(incidentService);
    }

    @Test
    void viewerShouldReceive403WhenAddingIncidentUpdate()
            throws Exception {
        mockAuthenticatedUser(23L, UserRole.VIEWER, "viewer-update-token");

        mockMvc.perform(post("/api/incidents/1/updates")
                        .header(
                                "Authorization",
                                "Bearer viewer-update-token"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "message": "Investigation update"
                            }
                            """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(incidentService);
    }



    @Test
    void adminShouldBeAllowedToCreateService() throws Exception {
        mockAuthenticatedUser(20L, UserRole.ADMIN, "admin-service-token");

        when(serviceManagementService.createService(any()))
                .thenReturn(new com.roshan.devopshub.service.ServiceResponse(
                        1L,
                        "Payments",
                        "Payment processing",
                        java.time.Instant.parse("2026-01-01T10:00:00Z"),
                        java.time.Instant.parse("2026-01-01T10:00:00Z")
                ));

        mockMvc.perform(post("/api/services")
                        .header("Authorization", "Bearer admin-service-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Payments",
                              "description": "Payment processing"
                            }
                            """))
                .andExpect(status().isCreated());

        verify(serviceManagementService).createService(any());
    }

    @Test
    void responderShouldReceive403WhenCreatingService() throws Exception {
        mockAuthenticatedUser(
                21L, UserRole.RESPONDER, "responder-service-token"
        );

        mockMvc.perform(post("/api/services")
                        .header("Authorization", "Bearer responder-service-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Payments",
                              "description": "Payment processing"
                            }
                            """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(serviceManagementService);
    }

    @Test
    void viewerShouldReceive403WhenCreatingService() throws Exception {
        mockAuthenticatedUser(
                22L, UserRole.VIEWER, "viewer-service-token"
        );

        mockMvc.perform(post("/api/services")
                        .header("Authorization", "Bearer viewer-service-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Payments",
                              "description": "Payment processing"
                            }
                            """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(serviceManagementService);
    }
    @Test
    void viewerShouldBeAllowedToListServices() throws Exception {
        mockAuthenticatedUser(23L, UserRole.VIEWER, "viewer-read-services-token");

        when(serviceManagementService.getAllServices())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/services")
                        .header(
                                "Authorization",
                                "Bearer viewer-read-services-token"
                        ))
                .andExpect(status().isOk());

        verify(serviceManagementService).getAllServices();
    }


    @Test
    void protectedEndpointShouldReturn401ForExpiredToken()
            throws Exception {

        String token = "expired-test-token";

        when(jwtService.parseAccessToken(token))
                .thenThrow(new ExpiredJwtException(
                        null,
                        Jwts.claims().build(),
                        "Token has expired"
                ));

        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        verify(jwtService).parseAccessToken(token);
        verifyNoInteractions(incidentService);
    }

}
