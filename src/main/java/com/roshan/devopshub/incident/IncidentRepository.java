package com.roshan.devopshub.incident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @Query("""
            SELECT i FROM Incident i
            WHERE (:status IS NULL OR i.status = :status)
              AND (:severity IS NULL OR i.severity = :severity)
            """)
    Page<Incident> findByFilters(
            @Param("status") IncidentStatus status,
            @Param("severity") IncidentSeverity severity,
            Pageable pageable
    );
}