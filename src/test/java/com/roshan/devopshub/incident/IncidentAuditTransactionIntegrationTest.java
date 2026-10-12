package com.roshan.devopshub.incident;

import com.roshan.devopshub.audit.AuditEvent;
import com.roshan.devopshub.audit.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.roshan.devopshub.user.User;
import com.roshan.devopshub.user.UserRole;
import com.roshan.devopshub.user.UserRepository;


@SpringBootTest
class IncidentAuditTransactionIntegrationTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    private User createTestActor(String prefix) {
        String email = prefix + "-" + java.util.UUID.randomUUID()
                + "@example.com";
        return userRepository.save(
                new User(email, "Integration Test Actor", UserRole.RESPONDER)
        );
    }

    @Test
    void shouldRollbackStatusChangeWhenAuditInsertFails() {
        User actor = createTestActor("status-rollback");
        Long actorId = actor.getId();

        Incident incident = incidentService.createIncident(
                actorId,
                "Database connection failure",
                "Integration test incident",
                IncidentSeverity.HIGH
        );

        Long incidentId = incident.getId();

        // Establish the initial persisted state for this test.
        incidentService.transitionStatus(
                incidentId,
                IncidentStatus.IN_PROGRESS,
                actorId
        );

        installFailingAuditTrigger();

        try {
            // The audit insert will fail during this transition.
            assertThrows(
                    DataAccessException.class,
                    () -> incidentService.transitionStatus(
                            incidentId,
                            IncidentStatus.RESOLVED,
                            actorId
                    )
            );

            // Read from the database after the service transaction has failed.
            Incident persistedIncident = incidentRepository
                    .findById(incidentId)
                    .orElseThrow();

            assertEquals(
                    IncidentStatus.IN_PROGRESS,
                    persistedIncident.getStatus(),
                    "The incident status change should have rolled back"
            );

            List<AuditEvent> events =
                    auditEventRepository
                            .findByIncidentIdOrderByCreatedAtAscIdAsc(
                                    incidentId
                            );

            assertTrue(
                    events.stream().noneMatch(event ->
                            "IN_PROGRESS -> RESOLVED".equals(
                                    event.getDetails()
                            )
                    ),
                    "The failed status transition must not have an audit event"
            );
        } finally {
            removeFailingAuditTrigger();
        }
    }

    private void installFailingAuditTrigger() {
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION
                    devopshub_test_fail_status_audit_insert()
                RETURNS trigger AS $$
                BEGIN
                    IF NEW.action = 'STATUS_CHANGED'
                       AND NEW.details = 'IN_PROGRESS -> RESOLVED' THEN
                        RAISE EXCEPTION
                            'Intentional audit failure for integration test';
                    END IF;

                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);

        jdbcTemplate.execute("""
                CREATE TRIGGER
                    devopshub_test_fail_status_audit_trigger
                BEFORE INSERT ON audit_events
                FOR EACH ROW
                EXECUTE FUNCTION
                    devopshub_test_fail_status_audit_insert()
                """);
    }

    private void removeFailingAuditTrigger() {
        jdbcTemplate.execute("""
                DROP TRIGGER IF EXISTS
                    devopshub_test_fail_status_audit_trigger
                ON audit_events
                """);

        jdbcTemplate.execute("""
                DROP FUNCTION IF EXISTS
                    devopshub_test_fail_status_audit_insert()
                """);
    }

    @Test
    void shouldRollbackIncidentCreationWhenAuditInsertFails() {
        User actor = createTestActor("creation-rollback");
        Long actorId = actor.getId();
        String title = "Rollback test " + java.util.UUID.randomUUID();

        try {
            installFailingCreationAuditTrigger();

            assertThrows(
                    DataAccessException.class,
                    () -> incidentService.createIncident(
                            actorId,
                            title,
                            "The audit insert should fail.",
                            IncidentSeverity.HIGH
                    )
            );

            Integer incidentCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM incidents WHERE title = ?",
                    Integer.class,
                    title
            );

            assertEquals(
                    0,
                    incidentCount,
                    "The incident insert should have rolled back"
            );

            Integer auditCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM audit_events ae
                    JOIN incidents i ON i.id = ae.incident_id
                    WHERE i.title = ?
                      AND ae.action = 'INCIDENT_CREATED'
                    """,
                    Integer.class,
                    title
            );

            assertEquals(
                    0,
                    auditCount,
                    "No creation audit event should remain for the failed incident"
            );
        } finally {
            removeFailingCreationAuditTrigger();
        }
    }

    private void installFailingCreationAuditTrigger() {
        jdbcTemplate.execute("""
            CREATE OR REPLACE FUNCTION
                devopshub_test_fail_creation_audit_insert()
            RETURNS trigger AS $$
            BEGIN
                IF NEW.action = 'INCIDENT_CREATED' THEN
                    RAISE EXCEPTION
                        'Intentional creation audit failure for integration test';
                END IF;

                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql
            """);

        jdbcTemplate.execute("""
            CREATE TRIGGER
                devopshub_test_fail_creation_audit_trigger
            BEFORE INSERT ON audit_events
            FOR EACH ROW
            EXECUTE FUNCTION
                devopshub_test_fail_creation_audit_insert()
            """);
    }

    private void removeFailingCreationAuditTrigger() {
        jdbcTemplate.execute("""
            DROP TRIGGER IF EXISTS
                devopshub_test_fail_creation_audit_trigger
            ON audit_events
            """);

        jdbcTemplate.execute("""
            DROP FUNCTION IF EXISTS
                devopshub_test_fail_creation_audit_insert()
            """);
    }

    @Test
    void shouldPersistIncidentAssignmentAndAuditEvent() {
        User actor = createTestActor("assignment-actor");
        Long actorId = actor.getId();
        String email = "assignment-" + java.util.UUID.randomUUID()
                + "@example.com";

        User user = userRepository.save(
                new User(email, "Test Responder", UserRole.RESPONDER)
        );

        Incident incident = incidentService.createIncident(
                actorId,
                "Assignment integration test",
                "Verify assignment persistence.",
                IncidentSeverity.HIGH
        );

        incidentService.assignIncident(
                incident.getId(), user.getId(), actorId);

        Incident persistedIncident = incidentRepository
                .findById(incident.getId())
                .orElseThrow();

        assertEquals(user.getId(), persistedIncident.getAssignee().getId());

        List<AuditEvent> events = auditEventRepository
                .findByIncidentIdOrderByCreatedAtAscIdAsc(incident.getId());

        assertTrue(events.stream().anyMatch(event ->
                "INCIDENT_ASSIGNED".equals(event.getAction())
                        && event.getDetails().equals(
                        "Unassigned -> " + email
                )
        ));
    }


    @Test
    void shouldRollbackIncidentReassignmentWhenAuditInsertFails() {
        User actor = createTestActor("reassignment-actor");
        Long actorId = actor.getId();
        String firstEmail = "first-" + java.util.UUID.randomUUID()
                + "@example.com";
        String secondEmail = "second-" + java.util.UUID.randomUUID()
                + "@example.com";

        User firstUser = userRepository.save(
                new User(firstEmail, "First Responder", UserRole.RESPONDER)
        );

        User secondUser = userRepository.save(
                new User(secondEmail, "Second Responder", UserRole.RESPONDER)
        );

        Incident incident = incidentService.createIncident(
                actorId,
                "Assignment rollback integration test",
                "Verify reassignment rollback.",
                IncidentSeverity.HIGH
        );

        Long incidentId = incident.getId();

        // Establish the initial persisted assignee.
        incidentService.assignIncident(incidentId, firstUser.getId(), actorId);

        installFailingAssignmentAuditTrigger();

        try {
            assertThrows(
                    DataAccessException.class,
                    () -> incidentService.assignIncident(
                            incidentId,
                            secondUser.getId(),
                            actorId
                    )
            );

            // Reload the incident from PostgreSQL after the failed transaction.
            Incident persistedIncident = incidentRepository
                    .findById(incidentId)
                    .orElseThrow();

            assertEquals(
                    firstUser.getId(),
                    persistedIncident.getAssignee().getId(),
                    "The original assignee should remain after rollback"
            );

            List<AuditEvent> events = auditEventRepository
                    .findByIncidentIdOrderByCreatedAtAscIdAsc(incidentId);

            assertTrue(
                    events.stream().noneMatch(event ->
                            "INCIDENT_ASSIGNED".equals(event.getAction())
                                    && event.getDetails().equals(
                                    firstEmail + " -> " + secondEmail
                            )
                    ),
                    "The failed reassignment must not have an audit event"
            );
        } finally {
            removeFailingAssignmentAuditTrigger();
        }
    }


    private void installFailingAssignmentAuditTrigger() {
        jdbcTemplate.execute("""
            CREATE OR REPLACE FUNCTION
                devopshub_test_fail_assignment_audit_insert()
            RETURNS trigger AS $$
            BEGIN
                IF NEW.action = 'INCIDENT_ASSIGNED' THEN
                    RAISE EXCEPTION
                        'Intentional assignment audit failure for integration test';
                END IF;

                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql
            """);

        jdbcTemplate.execute("""
            CREATE TRIGGER
                devopshub_test_fail_assignment_audit_trigger
            BEFORE INSERT ON audit_events
            FOR EACH ROW
            EXECUTE FUNCTION
                devopshub_test_fail_assignment_audit_insert()
            """);
    }


    @Test
    void shouldRollbackInvestigationUpdateWhenAuditInsertFails() {
        User actor = createTestActor("update-rollback");
        Long actorId = actor.getId();

        Incident incident = incidentService.createIncident(
                actorId,
                "Investigation update rollback test",
                "Verify update and audit rollback.",
                IncidentSeverity.HIGH
        );

        Long incidentId = incident.getId();
        String message = "Investigation update " + java.util.UUID.randomUUID();

        try {
            installFailingInvestigationUpdateAuditTrigger();

            assertThrows(
                    DataAccessException.class,
                    () -> incidentService.addUpdate(
                            incidentId,
                            message,
                            actorId
                    )
            );

            Integer updateCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM incident_updates
                    WHERE incident_id = ? AND message = ?
                    """,
                    Integer.class,
                    incidentId,
                    message
            );

            assertEquals(
                    0,
                    updateCount,
                    "The investigation update should have rolled back"
            );

            Integer auditCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM audit_events
                    WHERE incident_id = ?
                      AND action = 'INVESTIGATION_UPDATE'
                      AND details = ?
                    """,
                    Integer.class,
                    incidentId,
                    message
            );

            assertEquals(
                    0,
                    auditCount,
                    "The failed investigation update must not have an audit event"
            );
        } finally {
            removeFailingInvestigationUpdateAuditTrigger();
        }
    }

    private void installFailingInvestigationUpdateAuditTrigger() {
        jdbcTemplate.execute("""
            CREATE OR REPLACE FUNCTION
                devopshub_test_fail_investigation_update_audit_insert()
            RETURNS trigger AS $$
            BEGIN
                IF NEW.action = 'INVESTIGATION_UPDATE' THEN
                    RAISE EXCEPTION
                        'Intentional investigation update audit failure';
                END IF;

                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql
            """);

        jdbcTemplate.execute("""
            CREATE TRIGGER
                devopshub_test_fail_investigation_update_audit_trigger
            BEFORE INSERT ON audit_events
            FOR EACH ROW
            EXECUTE FUNCTION
                devopshub_test_fail_investigation_update_audit_insert()
            """);
    }

    private void removeFailingInvestigationUpdateAuditTrigger() {
        jdbcTemplate.execute("""
            DROP TRIGGER IF EXISTS
                devopshub_test_fail_investigation_update_audit_trigger
            ON audit_events
            """);

        jdbcTemplate.execute("""
            DROP FUNCTION IF EXISTS
                devopshub_test_fail_investigation_update_audit_insert()
            """);
    }


    private void removeFailingAssignmentAuditTrigger() {
        jdbcTemplate.execute("""
            DROP TRIGGER IF EXISTS
                devopshub_test_fail_assignment_audit_trigger
            ON audit_events
            """);

        jdbcTemplate.execute("""
            DROP FUNCTION IF EXISTS
                devopshub_test_fail_assignment_audit_insert()
            """);
    }

}