
package com.roshan.devopshub.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository
        extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByIncidentIdOrderByCreatedAtAscIdAsc(
            Long incidentId
    );
}
