
package com.roshan.devopshub.incident;

import org.springframework.data.jpa.repository.JpaRepository;
    
import java.util.List;

public interface IncidentUpdateRepository
        extends JpaRepository<IncidentUpdate, Long> {

    List<IncidentUpdate> findByIncidentIdOrderByCreatedAtAsc(
            Long incidentId
    );
}
