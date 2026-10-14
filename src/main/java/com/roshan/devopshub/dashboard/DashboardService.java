package com.roshan.devopshub.dashboard;

import com.roshan.devopshub.incident.IncidentRepository;
import com.roshan.devopshub.incident.IncidentSeverity;
import com.roshan.devopshub.incident.IncidentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final IncidentRepository incidentRepository;

    public DashboardService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        Map<IncidentStatus, Long> statusCounts =
                toStatusCountMap(
                        incidentRepository.countIncidentsByStatus()
                );

        Map<IncidentSeverity, Long> severityCounts =
                toSeverityCountMap(
                        incidentRepository.countIncidentsBySeverity()
                );

        long open = statusCounts.getOrDefault(
                IncidentStatus.OPEN, 0L
        );
        long inProgress = statusCounts.getOrDefault(
                IncidentStatus.IN_PROGRESS, 0L
        );
        long resolved = statusCounts.getOrDefault(
                IncidentStatus.RESOLVED, 0L
        );

        return new DashboardSummaryResponse(
                open + inProgress + resolved,
                open,
                inProgress,
                resolved,
                severityCounts.getOrDefault(
                        IncidentSeverity.LOW, 0L
                ),
                severityCounts.getOrDefault(
                        IncidentSeverity.MEDIUM, 0L
                ),
                severityCounts.getOrDefault(
                        IncidentSeverity.HIGH, 0L
                ),
                severityCounts.getOrDefault(
                        IncidentSeverity.CRITICAL, 0L
                )
        );
    }

    private Map<IncidentStatus, Long> toStatusCountMap(
            List<Object[]> rows
    ) {
        Map<IncidentStatus, Long> counts =
                new EnumMap<>(IncidentStatus.class);

        for (Object[] row : rows) {
            IncidentStatus status = (IncidentStatus) row[0];
            long count = ((Number) row[1]).longValue();
            counts.put(status, count);
        }

        return counts;
    }

    private Map<IncidentSeverity, Long> toSeverityCountMap(
            List<Object[]> rows
    ) {
        Map<IncidentSeverity, Long> counts =
                new EnumMap<>(IncidentSeverity.class);

        for (Object[] row : rows) {
            IncidentSeverity severity = (IncidentSeverity) row[0];
            long count = ((Number) row[1]).longValue();
            counts.put(severity, count);
        }

        return counts;
    }
}
