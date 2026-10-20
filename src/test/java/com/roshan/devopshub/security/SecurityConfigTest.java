
package com.roshan.devopshub.security;

import com.roshan.devopshub.auth.AuthService;
import com.roshan.devopshub.auth.LoginRequest;
import com.roshan.devopshub.auth.LoginResponse;
import com.roshan.devopshub.incident.*;
import com.roshan.devopshub.service.ServiceResponse;
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
import com.roshan.devopshub.user.UserResponse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;


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

    @MockitoBean
    private com.roshan.devopshub.dashboard.DashboardService dashboardService;

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

        when(incidentService.createIncident(11L, title, description, severity,null))
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

        verify(incidentService).createIncident(11L, title, description, severity,null);
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
                isNull(),
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
                isNull(),
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

    @Test
    void viewerShouldBeAllowedToGetServiceById() throws Exception {
        mockAuthenticatedUser(
                24L, UserRole.VIEWER, "viewer-get-service-token"
        );

        ServiceResponse service = new ServiceResponse(
                1L,
                "Payments",
                "Payment processing",
                java.time.Instant.parse("2026-01-01T10:00:00Z"),
                java.time.Instant.parse("2026-01-01T10:00:00Z")
        );

        when(serviceManagementService.getServiceById(1L))
                .thenReturn(service);

        mockMvc.perform(get("/api/services/1")
                        .header(
                                "Authorization",
                                "Bearer viewer-get-service-token"
                        ))
                .andExpect(status().isOk());

        verify(serviceManagementService).getServiceById(1L);
    }
    @Test
    void currentUserEndpointShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentUserEndpointShouldReturnAuthenticatedUserProfile()
            throws Exception {

        String token = "current-user-profile-token";
        mockAuthenticatedUser(31L, UserRole.VIEWER, token);

        User user = mock(User.class);
        when(user.getId()).thenReturn(31L);
        when(user.getEmail()).thenReturn("viewer@example.com");
        when(user.getDisplayName()).thenReturn("Test Viewer");
        when(user.getRole()).thenReturn(UserRole.VIEWER);
        when(user.getCreatedAt())
                .thenReturn(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        when(userRepository.findById(31L)).thenReturn(java.util.Optional.of(user));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.email").value("viewer@example.com"))
                .andExpect(jsonPath("$.displayName").value("Test Viewer"))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(userRepository, org.mockito.Mockito.atLeastOnce())
                .findById(31L);
    }

    @Test
    void dashboardShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(dashboardService);
    }

    @Test
    void adminShouldBeAllowedToReadDashboard() throws Exception {
        mockAuthenticatedUser(41L, UserRole.ADMIN, "dashboard-admin-token");
        stubDashboardSummary();

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer dashboard-admin-token"))
                .andExpect(status().isOk());

        verify(dashboardService).getSummary();
    }

    @Test
    void responderShouldBeAllowedToReadDashboard() throws Exception {
        mockAuthenticatedUser(
                42L, UserRole.RESPONDER, "dashboard-responder-token"
        );
        stubDashboardSummary();

        mockMvc.perform(get("/api/dashboard/summary")
                        .header(
                                "Authorization",
                                "Bearer dashboard-responder-token"
                        ))
                .andExpect(status().isOk());

        verify(dashboardService).getSummary();
    }

    @Test
    void viewerShouldBeAllowedToReadDashboard() throws Exception {
        mockAuthenticatedUser(43L, UserRole.VIEWER, "dashboard-viewer-token");
        stubDashboardSummary();

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer dashboard-viewer-token"))
                .andExpect(status().isOk());

        verify(dashboardService).getSummary();
    }

    private void stubDashboardSummary() {
        when(dashboardService.getSummary()).thenReturn(
                new com.roshan.devopshub.dashboard.DashboardSummaryResponse(
                        3L, 1L, 1L, 1L, 0L, 1L, 1L, 1L
                )
        );
    }

    @Test
    void adminShouldBeAllowedToAssignIncident() throws Exception {
        String token = "admin-assignment-token";

        mockAuthenticatedUser(101L, UserRole.ADMIN, token);

        Incident incident = new Incident(
                "API gateway failure",
                "Requests are failing.",
                IncidentSeverity.HIGH
        );

        when(incidentService.assignIncident(17L, 2L, 101L))
                .thenReturn(incident);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .put("/api/incidents/17/assignee")
                                .header(
                                        org.springframework.http.HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(
                                        org.springframework.http.MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "userId": 2
                                    }
                                    """)
                )
                .andExpect(status().isOk());

        verify(incidentService).assignIncident(17L, 2L, 101L);
    }

    @Test
    void responderShouldBeAllowedToAssignIncident() throws Exception {
        String token = "responder-assignment-token";

        mockAuthenticatedUser(102L, UserRole.RESPONDER, token);

        Incident incident = new Incident(
                "API gateway failure",
                "Requests are failing.",
                IncidentSeverity.HIGH
        );

        when(incidentService.assignIncident(17L, 2L, 102L))
                .thenReturn(incident);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .put("/api/incidents/17/assignee")
                                .header(
                                        org.springframework.http.HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(
                                        org.springframework.http.MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "userId": 2
                                    }
                                    """)
                )
                .andExpect(status().isOk());

        verify(incidentService).assignIncident(17L, 2L, 102L);
    }

    @Test
    void viewerShouldReceive403WhenChangingIncidentSeverity()
            throws Exception {
        mockAuthenticatedUser(
                24L,
                UserRole.VIEWER,
                "viewer-severity-token"
        );

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .patch("/api/incidents/1/severity")
                                .header(
                                        "Authorization",
                                        "Bearer viewer-severity-token"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "severity": "CRITICAL"
                                    }
                                    """)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(incidentService);
    }

    @Test
    void adminShouldBeAllowedToChangeIncidentSeverity() throws Exception {
        String token = "admin-severity-token";

        mockAuthenticatedUser(201L, UserRole.ADMIN, token);

        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.CRITICAL
        );

        when(incidentService.changeSeverity(
                17L,
                IncidentSeverity.CRITICAL,
                201L
        )).thenReturn(incident);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .patch("/api/incidents/17/severity")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "severity": "CRITICAL"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("CRITICAL"));

        verify(incidentService).changeSeverity(
                17L,
                IncidentSeverity.CRITICAL,
                201L
        );
    }

    @Test
    void responderShouldBeAllowedToChangeIncidentSeverity()
            throws Exception {
        String token = "responder-severity-token";

        mockAuthenticatedUser(202L, UserRole.RESPONDER, token);

        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.CRITICAL
        );

        when(incidentService.changeSeverity(
                17L,
                IncidentSeverity.CRITICAL,
                202L
        )).thenReturn(incident);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .patch("/api/incidents/17/severity")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "severity": "CRITICAL"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("CRITICAL"));

        verify(incidentService).changeSeverity(
                17L,
                IncidentSeverity.CRITICAL,
                202L
        );
    }

    @Test
    void adminShouldBeAllowedToChangeIncidentStatus() throws Exception {
        String token = "admin-transition-token";
        mockAuthenticatedUser(301L, UserRole.ADMIN, token);

        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );
        incident.transitionTo(IncidentStatus.IN_PROGRESS);

        when(incidentService.transitionStatus(
                17L, IncidentStatus.IN_PROGRESS, 301L
        )).thenReturn(incident);

        mockMvc.perform(post("/api/incidents/17/transitions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "status": "IN_PROGRESS"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(incidentService).transitionStatus(
                17L, IncidentStatus.IN_PROGRESS, 301L
        );
    }

    @Test
    void responderShouldBeAllowedToChangeIncidentStatus()
            throws Exception {
        String token = "responder-transition-token";
        mockAuthenticatedUser(302L, UserRole.RESPONDER, token);

        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );
        incident.transitionTo(IncidentStatus.IN_PROGRESS);

        when(incidentService.transitionStatus(
                17L, IncidentStatus.IN_PROGRESS, 302L
        )).thenReturn(incident);

        mockMvc.perform(post("/api/incidents/17/transitions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "status": "IN_PROGRESS"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(incidentService).transitionStatus(
                17L, IncidentStatus.IN_PROGRESS, 302L
        );
    }

    @Test
    void adminShouldBeAllowedToAddIncidentUpdate() throws Exception {
        String token = "admin-update-token";
        mockAuthenticatedUser(303L, UserRole.ADMIN, token);

        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );
        IncidentUpdate update = new IncidentUpdate(
                incident,
                "Database connection pool exhaustion identified."
        );

        when(incidentService.addUpdate(
                17L,
                "Database connection pool exhaustion identified.",
                303L
        )).thenReturn(update);

        mockMvc.perform(post("/api/incidents/17/updates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "message": "Database connection pool exhaustion identified."
                            }
                            """))
                .andExpect(status().isCreated());

        verify(incidentService).addUpdate(
                17L,
                "Database connection pool exhaustion identified.",
                303L
        );
    }

    @Test
    void responderShouldBeAllowedToAddIncidentUpdate()
            throws Exception {
        String token = "responder-update-token";
        mockAuthenticatedUser(304L, UserRole.RESPONDER, token);

        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );
        IncidentUpdate update = new IncidentUpdate(
                incident,
                "Database connection pool exhaustion identified."
        );

        when(incidentService.addUpdate(
                17L,
                "Database connection pool exhaustion identified.",
                304L
        )).thenReturn(update);

        mockMvc.perform(post("/api/incidents/17/updates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "message": "Database connection pool exhaustion identified."
                            }
                            """))
                .andExpect(status().isCreated());

        verify(incidentService).addUpdate(
                17L,
                "Database connection pool exhaustion identified.",
                304L
        );
    }

    @Test
    void assignableUsersEndpointShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/assignable"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminShouldBeAllowedToListAssignableUsers() throws Exception {
        String token = "admin-assignable-users-token";
        mockAuthenticatedUser(51L, UserRole.ADMIN, token);

        when(userRepository.findByRoleInOrderByDisplayNameAsc(
                List.of(UserRole.ADMIN, UserRole.RESPONDER)
        )).thenReturn(List.of());

        mockMvc.perform(get("/api/users/assignable")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(userRepository).findByRoleInOrderByDisplayNameAsc(
                List.of(UserRole.ADMIN, UserRole.RESPONDER)
        );
    }

    @Test
    void responderShouldBeAllowedToListAssignableUsers() throws Exception {
        String token = "responder-assignable-users-token";
        mockAuthenticatedUser(52L, UserRole.RESPONDER, token);

        when(userRepository.findByRoleInOrderByDisplayNameAsc(
                List.of(UserRole.ADMIN, UserRole.RESPONDER)
        )).thenReturn(List.of());

        mockMvc.perform(get("/api/users/assignable")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void viewerShouldReceive403WhenListingAssignableUsers()
            throws Exception {
        String token = "viewer-assignable-users-token";
        mockAuthenticatedUser(53L, UserRole.VIEWER, token);

        mockMvc.perform(get("/api/users/assignable")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verify(userRepository, never())
                .findByRoleInOrderByDisplayNameAsc(anyCollection());
    }

}
