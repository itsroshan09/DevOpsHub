
package com.roshan.devopshub.config;

import com.roshan.devopshub.audit.AuditEvent;
import com.roshan.devopshub.audit.AuditEventRepository;
import com.roshan.devopshub.incident.Incident;
import com.roshan.devopshub.incident.IncidentRepository;
import com.roshan.devopshub.incident.IncidentSeverity;
import com.roshan.devopshub.incident.IncidentStatus;
import com.roshan.devopshub.incident.IncidentUpdate;
import com.roshan.devopshub.incident.IncidentUpdateRepository;
import com.roshan.devopshub.service.Service;
import com.roshan.devopshub.service.ServiceRepository;
import com.roshan.devopshub.user.User;
import com.roshan.devopshub.user.UserRepository;
import com.roshan.devopshub.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
@Configuration
@Profile("local")
public class DevelopmentDataSeeder {

    @Bean
    CommandLineRunner seedDevelopmentData(
            UserRepository userRepository,
            ServiceRepository serviceRepository,
            IncidentRepository incidentRepository,
            IncidentUpdateRepository updateRepository,
            AuditEventRepository auditRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            @Value("${devopshub.seed.enabled:false}") boolean enabled,
            @Value("${devopshub.seed.password:}") String seedPassword
    ) {
        return args -> {
            if (!enabled) {
                return;
            }

            if (seedPassword == null || seedPassword.isBlank()) {
                throw new IllegalStateException(
                        "Set devopshub.seed.password before seeding."
                );
            }

            transactionTemplate.executeWithoutResult(status ->
                    seed(
                            userRepository,
                            serviceRepository,
                            incidentRepository,
                            updateRepository,
                            auditRepository,
                            passwordEncoder,
                            jdbcTemplate,
                            seedPassword
                    )
            );
        };
    }


    void seed(
            UserRepository users,
            ServiceRepository services,
            IncidentRepository incidents,
            IncidentUpdateRepository updates,
            AuditEventRepository audits,
            PasswordEncoder encoder,
            JdbcTemplate jdbcTemplate, String rawPassword
    ) {
        if (users.count() != 0
                || services.count() != 0
                || incidents.count() != 0
                || updates.count() != 0
                || audits.count() != 0) {
            long userCount = users.count();
            long serviceCount = services.count();
            long incidentCount = incidents.count();
            long updateCount = updates.count();
            long auditCount = audits.count();

            System.out.println(
                    "Seed JDBC identity: " +
                            jdbcTemplate.queryForMap(
                                    "SELECT current_database() AS db, " +
                                            "current_schema() AS schema, " +
                                            "inet_server_addr() AS server_addr, " +
                                            "inet_server_port() AS server_port"
                            )
            );

            System.out.println(
                    "Direct JDBC users count: " +
                            jdbcTemplate.queryForObject(
                                    "SELECT COUNT(*) FROM public.users",
                                    Long.class
                            )
            );

            System.out.printf(
                    "Seed pre-check: users=%d, services=%d, incidents=%d, updates=%d, auditEvents=%d%n",
                    userCount,
                    serviceCount,
                    incidentCount,
                    updateCount,
                    auditCount
            );

            if (userCount != 0
                    || serviceCount != 0
                    || incidentCount != 0
                    || updateCount != 0
                    || auditCount != 0) {
                throw new IllegalStateException(
                        "Refusing to seed: application tables must all be empty."
                );
            }

            User admin = createUser(
                    users, encoder, "admin@devopshub.local",
                    "DevOps Admin", UserRole.ADMIN, rawPassword
            );
            User alex = createUser(
                    users, encoder, "alex.morgan@devopshub.local",
                    "Alex Morgan", UserRole.RESPONDER, rawPassword
            );
            User jamie = createUser(
                    users, encoder, "jamie.chen@devopshub.local",
                    "Jamie Chen", UserRole.RESPONDER, rawPassword
            );
            createUser(
                    users, encoder, "taylor.reed@devopshub.local",
                    "Taylor Reed", UserRole.VIEWER, rawPassword
            );

            services.save(new Service(
                    "Payment Service",
                    "Processes payments, refunds, and transaction status."
            ));
            services.save(new Service(
                    "Authentication Service",
                    "Handles login, token issuance, and identity verification."
            ));
            services.save(new Service(
                    "Notification Service",
                    "Delivers email and in-app notifications."
            ));

            Incident payment = createIncident(
                    incidents,
                    "Payment requests failing",
                    "A rise in payment failures followed the latest release.",
                    IncidentSeverity.CRITICAL,
                    IncidentStatus.IN_PROGRESS,
                    alex
            );

            Incident login = createIncident(
                    incidents,
                    "Intermittent login failures",
                    "Some users receive authentication errors during login.",
                    IncidentSeverity.HIGH,
                    IncidentStatus.OPEN,
                    jamie
            );

            Incident notifications = createIncident(
                    incidents,
                    "Notification delivery delayed",
                    "Queued notifications are taking longer than expected.",
                    IncidentSeverity.MEDIUM,
                    IncidentStatus.IN_PROGRESS,
                    jamie
            );

            Incident latency = createIncident(
                    incidents,
                    "Elevated API latency",
                    "Response times are above the normal operating range.",
                    IncidentSeverity.HIGH,
                    IncidentStatus.OPEN,
                    alex
            );

            Incident deployment = createIncident(
                    incidents,
                    "Deployment configuration issue resolved",
                    "A configuration mismatch caused a failed deployment.",
                    IncidentSeverity.LOW,
                    IncidentStatus.RESOLVED,
                    alex
            );

            addUpdate(updates, payment,
                    "Initial triage confirmed elevated payment error rates.");
            addUpdate(updates, payment,
                    "Rollback is being evaluated while logs are reviewed.");
            addUpdate(updates, login,
                    "Investigation started; authentication error logs are being checked.");
            addUpdate(updates, notifications,
                    "Queue backlog identified; processing metrics are under review.");
            addUpdate(updates, latency,
                    "API latency has increased; endpoint-level metrics are being compared.");
            addUpdate(updates, deployment,
                    "Configuration corrected and deployment verified.");

            addAudit(audits, payment, admin, "INCIDENT_CREATED",
                    "Seed incident created: payment requests failing.");
            addAudit(audits, payment, admin, "INCIDENT_ASSIGNED",
                    "Assigned to Alex Morgan.");
            addAudit(audits, payment, alex, "STATUS_CHANGED",
                    "Status set to IN_PROGRESS.");
            addAudit(audits, login, admin, "INCIDENT_CREATED",
                    "Seed incident created: intermittent login failures.");
            addAudit(audits, notifications, admin, "INCIDENT_CREATED",
                    "Seed incident created: notification delivery delayed.");
            addAudit(audits, latency, admin, "INCIDENT_CREATED",
                    "Seed incident created: elevated API latency.");
            addAudit(audits, deployment, admin, "INCIDENT_CREATED",
                    "Seed incident created: deployment configuration issue.");
            addAudit(audits, deployment, alex, "STATUS_CHANGED",
                    "Incident marked RESOLVED.");
        }
    }
    private User createUser(
            UserRepository repository,
            PasswordEncoder encoder,
            String email,
            String name,
            UserRole role,
            String rawPassword
    ) {
        User user = new User(email, name, role);
        user.setPasswordHash(encoder.encode(rawPassword));
        return repository.save(user);
    }

    private Incident createIncident(
            IncidentRepository repository,
            String title,
            String description,
            IncidentSeverity severity,
            IncidentStatus status,
            User assignee
    ) {
        Incident incident = new Incident(title, description, severity);
        incident.transitionTo(status);
        incident.assignTo(assignee);
        return repository.save(incident);
    }

    private void addUpdate(
            IncidentUpdateRepository repository,
            Incident incident,
            String message
    ) {
        repository.save(new IncidentUpdate(incident, message));
    }

    private void addAudit(
            AuditEventRepository repository,
            Incident incident,
            User actor,
            String action,
            String details
    ) {
        repository.save(new AuditEvent(incident, actor, action, details));
    }
}
