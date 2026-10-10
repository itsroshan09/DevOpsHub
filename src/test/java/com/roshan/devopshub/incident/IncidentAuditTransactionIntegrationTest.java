
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

    @Test
    void shouldRollbackStatusChangeWhenAuditInsertFails() {
        Incident incident = incidentService.createIncident(
                "Database connection failure",
                "Integration test incident",
                IncidentSeverity.HIGH
        );

        Long incidentId = incident.getId();

        // Establish the initial persisted state for this test.
        incidentService.transitionStatus(
                incidentId,
                IncidentStatus.IN_PROGRESS
        );

        installFailingAuditTrigger();

        try {
            // The audit insert will fail during this transition.
            assertThrows(
                    DataAccessException.class,
                    () -> incidentService.transitionStatus(
                            incidentId,
                            IncidentStatus.RESOLVED
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
}
