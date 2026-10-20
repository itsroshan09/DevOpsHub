
package com.roshan.devopshub.incident;

import com.roshan.devopshub.user.User;
import com.roshan.devopshub.user.UserRepository;
import com.roshan.devopshub.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class IncidentDashboardAggregationIntegrationTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCountIncidentsByStatusAndSeverity() {
        Map<IncidentStatus, Long> initialStatusCounts =
                toStatusCountMap(
                        incidentRepository.countIncidentsByStatus()
                );

        Map<IncidentSeverity, Long> initialSeverityCounts =
                toSeverityCountMap(
                        incidentRepository.countIncidentsBySeverity()
                );

        User actor = userRepository.save(
                new User(
                        "dashboard-" + UUID.randomUUID() + "@example.com",
                        "Dashboard Test Actor",
                        UserRole.RESPONDER
                )
        );

        Long actorId = actor.getId();

        incidentService.createIncident(
                actorId,
                "Dashboard low incident " + UUID.randomUUID(),
                "Low severity test incident",
                IncidentSeverity.LOW,
                null
        );

        Incident highIncident = incidentService.createIncident(
                actorId,
                "Dashboard high incident " + UUID.randomUUID(),
                "High severity test incident",
                IncidentSeverity.HIGH,
                null
        );

        incidentService.transitionStatus(
                highIncident.getId(),
                IncidentStatus.IN_PROGRESS,
                actorId
        );

        Map<IncidentStatus, Long> statusCounts =
                toStatusCountMap(
                        incidentRepository.countIncidentsByStatus()
                );

        Map<IncidentSeverity, Long> severityCounts =
                toSeverityCountMap(
                        incidentRepository.countIncidentsBySeverity()
                );

        assertEquals(
                initialStatusCounts.getOrDefault(IncidentStatus.OPEN, 0L) + 1,
                statusCounts.getOrDefault(IncidentStatus.OPEN, 0L)
        );

        assertEquals(
                initialStatusCounts.getOrDefault(
                        IncidentStatus.IN_PROGRESS, 0L
                ) + 1,
                statusCounts.getOrDefault(
                        IncidentStatus.IN_PROGRESS, 0L
                )
        );

        assertEquals(
                initialSeverityCounts.getOrDefault(IncidentSeverity.LOW, 0L) + 1,
                severityCounts.getOrDefault(IncidentSeverity.LOW, 0L)
        );

        assertEquals(
                initialSeverityCounts.getOrDefault(IncidentSeverity.HIGH, 0L) + 1,
                severityCounts.getOrDefault(IncidentSeverity.HIGH, 0L)
        );
    }

    private Map<IncidentStatus, Long> toStatusCountMap(
            List<Object[]> rows
    ) {
        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (IncidentStatus) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
    }

    private Map<IncidentSeverity, Long> toSeverityCountMap(
            List<Object[]> rows
    ) {
        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (IncidentSeverity) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
    }
}
