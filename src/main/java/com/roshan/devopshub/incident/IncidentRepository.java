package com.roshan.devopshub.incident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @EntityGraph(attributePaths = "assignee")
    @Query("SELECT i FROM Incident i WHERE i.id = :id")
    Optional<Incident> findWithAssigneeById(@Param("id") Long id);

    @EntityGraph(attributePaths = "assignee")
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
