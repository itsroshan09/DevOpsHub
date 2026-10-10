
package com.roshan.devopshub.incident;

import com.roshan.devopshub.audit.AuditEvent;
import com.roshan.devopshub.audit.AuditEventRepository;
import com.roshan.devopshub.user.User;
import com.roshan.devopshub.user.UserNotFoundException;
import com.roshan.devopshub.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.roshan.devopshub.user.UserRepository;
import static org.mockito.Mockito.mock;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;



@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentUpdateRepository incidentUpdateRepository;

    @Mock
    private AuditEventRepository auditEventRepository;

    private IncidentService incidentService;
    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(
                incidentRepository,
                incidentUpdateRepository,
                auditEventRepository,
                userRepository
        );
    }

    @Test
    void createIncidentShouldSaveAndAuditIncident() {
        Incident incident = new Incident(
                "API gateway returning 500 errors",
                "Checkout requests are failing intermittently.",
                IncidentSeverity.HIGH
        );

        when(incidentRepository.save(any(Incident.class)))
                .thenReturn(incident);

        Incident result = incidentService.createIncident(
                "API gateway returning 500 errors",
                "Checkout requests are failing intermittently.",
                IncidentSeverity.HIGH
        );

        assertNotNull(result);
        assertEquals("API gateway returning 500 errors", result.getTitle());
        assertEquals(IncidentSeverity.HIGH, result.getSeverity());
        assertEquals(IncidentStatus.OPEN, result.getStatus());

        var order = inOrder(incidentRepository, auditEventRepository);
        order.verify(incidentRepository).save(any(Incident.class));
        order.verify(auditEventRepository).save(any(AuditEvent.class));
    }


    @Test
    void getIncidentByIdShouldThrowExceptionWhenIncidentDoesNotExist() {
        when(incidentRepository.findWithAssigneeById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IncidentNotFoundException.class,
                () -> incidentService.getIncidentById(999L)
        );

        verify(incidentRepository).findWithAssigneeById(999L);
    }

    @Test
    void getIncidentsShouldRequestPageFromRepository() {
        PageRequest pageable = PageRequest.of(0, 10);

        Page<Incident> expectedPage = new PageImpl<>(
                List.of(new Incident(
                        "API gateway returning 500 errors",
                        "Checkout requests are failing intermittently.",
                        IncidentSeverity.HIGH
                )),
                pageable,
                1
        );

        when(incidentRepository.findAll(pageable))
                .thenReturn(expectedPage);

        Page<Incident> result = incidentService.getIncidents(pageable);

        assertSame(expectedPage, result);
        verify(incidentRepository).findAll(pageable);
    }


    @Test
    void transitionStatusShouldAllowOpenToInProgress() {
        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );

        when(incidentRepository.findById(17L))
                .thenReturn(Optional.of(incident));

        when(incidentRepository.save(incident))
                .thenReturn(incident);

        Incident result = incidentService.transitionStatus(
                17L,
                IncidentStatus.IN_PROGRESS
        );

        assertEquals(IncidentStatus.IN_PROGRESS, result.getStatus());

        verify(incidentRepository).save(incident);

        ArgumentCaptor<AuditEvent> captor =
                ArgumentCaptor.forClass(AuditEvent.class);

        verify(auditEventRepository).save(captor.capture());

        AuditEvent event = captor.getValue();

        assertEquals("STATUS_CHANGED", event.getAction());
        assertEquals("OPEN -> IN_PROGRESS", event.getDetails());
    }


    @Test
    void transitionStatusShouldAllowInProgressToResolved() {
        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );
        incident.transitionTo(IncidentStatus.IN_PROGRESS);

        when(incidentRepository.findById(17L))
                .thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident))
                .thenReturn(incident);

        Incident result = incidentService.transitionStatus(
                17L,
                IncidentStatus.RESOLVED
        );

        assertEquals(IncidentStatus.RESOLVED, result.getStatus());

        verify(incidentRepository).save(incident);
        verify(auditEventRepository).save(any(AuditEvent.class));
    }

    @Test
    void transitionStatusShouldRejectOpenToResolved() {
        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );

        when(incidentRepository.findById(17L))
                .thenReturn(Optional.of(incident));

        assertThrows(
                IllegalArgumentException.class,
                () -> incidentService.transitionStatus(
                        17L,
                        IncidentStatus.RESOLVED
                )
        );

        assertEquals(IncidentStatus.OPEN, incident.getStatus());

        verify(incidentRepository, never())
                .save(any(Incident.class));
        verify(auditEventRepository, never())
                .save(any(AuditEvent.class));
    }

    @Test
    void transitionStatusShouldThrowWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IncidentNotFoundException.class,
                () -> incidentService.transitionStatus(
                        999L,
                        IncidentStatus.IN_PROGRESS
                )
        );

        verify(incidentRepository).findById(999L);
        verify(incidentRepository, never()).save(any(Incident.class));
        verify(auditEventRepository, never())
                .save(any(AuditEvent.class));
    }

    @Test
    void addUpdateShouldSaveAndReturnUpdate() {
        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );

        IncidentUpdate update = new IncidentUpdate(
                incident,
                "Database connection pool exhaustion identified."
        );

        when(incidentRepository.findById(17L))
                .thenReturn(Optional.of(incident));
        when(incidentUpdateRepository.save(any(IncidentUpdate.class)))
                .thenReturn(update);

        IncidentUpdate result = incidentService.addUpdate(
                17L,
                "Database connection pool exhaustion identified."
        );

        assertNotNull(result);
        assertSame(update, result);
        assertEquals(incident, result.getIncident());
        assertEquals(
                "Database connection pool exhaustion identified.",
                result.getMessage()
        );

        verify(incidentRepository).findById(17L);
        verify(incidentUpdateRepository).save(any(IncidentUpdate.class));
    }

    @Test
    void addUpdateShouldThrowWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IncidentNotFoundException.class,
                () -> incidentService.addUpdate(
                        999L,
                        "Investigation update"
                )
        );

        verify(incidentRepository).findById(999L);
        verify(incidentUpdateRepository, never())
                .save(any(IncidentUpdate.class));
    }

    @Test
    void getIncidentUpdatesShouldReturnUpdatesInRepositoryOrder() {
        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );

        IncidentUpdate firstUpdate = new IncidentUpdate(
                incident,
                "Investigating database connections."
        );

        IncidentUpdate secondUpdate = new IncidentUpdate(
                incident,
                "Connection pool exhaustion confirmed."
        );

        List<IncidentUpdate> expectedUpdates =
                List.of(firstUpdate, secondUpdate);

        when(incidentRepository.findById(17L))
                .thenReturn(Optional.of(incident));
        when(incidentUpdateRepository
                .findByIncidentIdOrderByCreatedAtAsc(17L))
                .thenReturn(expectedUpdates);

        List<IncidentUpdate> result =
                incidentService.getIncidentUpdates(17L);

        assertEquals(2, result.size());
        assertSame(firstUpdate, result.get(0));
        assertSame(secondUpdate, result.get(1));

        verify(incidentRepository).findById(17L);
        verify(incidentUpdateRepository)
                .findByIncidentIdOrderByCreatedAtAsc(17L);
    }

    @Test
    void getIncidentUpdatesShouldThrowWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IncidentNotFoundException.class,
                () -> incidentService.getIncidentUpdates(999L)
        );

        verify(incidentRepository).findById(999L);
        verify(incidentUpdateRepository, never())
                .findByIncidentIdOrderByCreatedAtAsc(999L);
    }


    @Test
    void getIncidentHistoryShouldCombineAndSortEventsChronologically() {
        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );

        Instant earlier = Instant.parse("2026-10-11T10:00:00Z");
        Instant later = Instant.parse("2026-10-11T10:05:00Z");

        IncidentUpdate update = new IncidentUpdate(
                incident,
                "Investigating database connections."
        );

        AuditEvent auditEvent = new AuditEvent(
                incident,
                "STATUS_CHANGED",
                "OPEN -> IN_PROGRESS"
        );

        // Set timestamps deterministically for this unit test.
        setCreatedAt(update, later);
        setCreatedAt(auditEvent, earlier);

        when(incidentRepository.findById(17L))
                .thenReturn(Optional.of(incident));

        when(incidentUpdateRepository
                .findByIncidentIdOrderByCreatedAtAsc(17L))
                .thenReturn(List.of(update));

        when(auditEventRepository
                .findByIncidentIdOrderByCreatedAtAscIdAsc(17L))
                .thenReturn(List.of(auditEvent));

        List<IncidentHistoryItemResponse> history =
                incidentService.getIncidentHistory(17L);

        assertEquals(2, history.size());
        assertEquals("STATUS_CHANGED", history.get(0).type());
        assertEquals("OPEN -> IN_PROGRESS", history.get(0).details());
        assertEquals(earlier, history.get(0).createdAt());

        assertEquals("INVESTIGATION_UPDATE", history.get(1).type());
        assertEquals(
                "Investigating database connections.",
                history.get(1).details()
        );
        assertEquals(later, history.get(1).createdAt());

        verify(incidentRepository).findById(17L);
        verify(incidentUpdateRepository)
                .findByIncidentIdOrderByCreatedAtAsc(17L);
        verify(auditEventRepository)
                .findByIncidentIdOrderByCreatedAtAscIdAsc(17L);
    }

    private void setCreatedAt(Object entity, Instant timestamp) {
        try {
            var field = entity.getClass().getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, timestamp);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    "Could not set test timestamp", exception
            );
        }
    }

    @Test
    void getIncidentHistoryShouldThrowWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IncidentNotFoundException.class,
                () -> incidentService.getIncidentHistory(999L)
        );

        verify(incidentRepository).findById(999L);
        verify(incidentUpdateRepository, never())
                .findByIncidentIdOrderByCreatedAtAsc(999L);
        verify(auditEventRepository, never())
                .findByIncidentIdOrderByCreatedAtAscIdAsc(999L);
    }

    @Test
    void createIncidentShouldRecordCorrectAuditDetails() {
        Incident incident = new Incident(
                "API gateway failure",
                "Requests are failing.",
                IncidentSeverity.HIGH
        );

        when(incidentRepository.save(any(Incident.class)))
                .thenReturn(incident);

        incidentService.createIncident(
                "API gateway failure",
                "Requests are failing.",
                IncidentSeverity.HIGH
        );

        ArgumentCaptor<AuditEvent> captor =
                ArgumentCaptor.forClass(AuditEvent.class);

        verify(auditEventRepository).save(captor.capture());

        AuditEvent event = captor.getValue();

        assertSame(incident, event.getIncident());
        assertEquals("INCIDENT_CREATED", event.getAction());
        assertEquals("Incident created", event.getDetails());
    }

    @Test
    void assignIncidentShouldAssignUserAndRecordAuditEvent() {
        Incident incident = new Incident(
                "API gateway failure",
                "Requests are failing.",
                IncidentSeverity.HIGH
        );

        User user = new User(
                "responder@example.com",
                "Test Responder",
                UserRole.RESPONDER
        );

        when(incidentRepository.findById(1L))
                .thenReturn(Optional.of(incident));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));
        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Incident result = incidentService.assignIncident(1L, 2L);

        assertSame(user, result.getAssignee());

        ArgumentCaptor<AuditEvent> captor =
                ArgumentCaptor.forClass(AuditEvent.class);

        verify(auditEventRepository).save(captor.capture());

        AuditEvent event = captor.getValue();
        assertSame(incident, event.getIncident());
        assertEquals("INCIDENT_ASSIGNED", event.getAction());
        assertEquals(
                "Unassigned -> responder@example.com",
                event.getDetails()
        );
    }

    @Test
    void assignIncidentShouldThrowWhenUserDoesNotExist() {
        when(incidentRepository.findById(1L))
                .thenReturn(Optional.of(new Incident(
                        "API gateway failure",
                        "Requests are failing.",
                        IncidentSeverity.HIGH
                )));
        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> incidentService.assignIncident(1L, 2L)
        );

        verify(incidentRepository, never()).save(any(Incident.class));
        verify(auditEventRepository, never()).save(any(AuditEvent.class));
    }

    @Test
    void assignIncidentShouldThrowWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IncidentNotFoundException.class,
                () -> incidentService.assignIncident(1L, 2L)
        );

        verify(userRepository, never()).findById(anyLong());
        verify(auditEventRepository, never()).save(any(AuditEvent.class));
    }
}
