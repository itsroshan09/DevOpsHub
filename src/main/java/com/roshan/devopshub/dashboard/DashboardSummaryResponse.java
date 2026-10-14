
package com.roshan.devopshub.dashboard;

public record DashboardSummaryResponse(
        long totalIncidents,
        long openIncidents,
        long inProgressIncidents,
        long resolvedIncidents,
        long lowSeverityIncidents,
        long mediumSeverityIncidents,
        long highSeverityIncidents,
        long criticalSeverityIncidents
) {
}
