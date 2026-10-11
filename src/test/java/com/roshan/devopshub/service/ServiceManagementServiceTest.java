package com.roshan.devopshub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceManagementServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    private ServiceManagementService serviceManagementService;

    @BeforeEach
    void setUp() {
        serviceManagementService =
                new ServiceManagementService(serviceRepository);
    }

    @Test
    void createServiceTrimsNameAndReturnsResponse() {
        Service savedService =
                new Service("Payments API", "Handles payments");

        when(serviceRepository.existsByName("Payments API"))
                .thenReturn(false);

        when(serviceRepository.save(any(Service.class)))
                .thenReturn(savedService);

        ServiceResponse response = serviceManagementService.createService(
                new ServiceCreateRequest(
                        "  Payments API  ",
                        "Handles payments"
                )
        );

        assertEquals("Payments API", response.name());
        assertEquals("Handles payments", response.description());

        verify(serviceRepository).existsByName("Payments API");
        verify(serviceRepository).save(any(Service.class));
    }
    @Test
    void createServiceRejectsDuplicateName() {
        ServiceCreateRequest request =
                new ServiceCreateRequest("Payments", "Payment service");

        when(serviceRepository.existsByName("Payments")).thenReturn(true);

        assertThrows(
                DuplicateServiceNameException.class,
                () -> serviceManagementService.createService(request)
        );

        verify(serviceRepository, never()).save(any(Service.class));
    }

    @Test
    void getServiceByIdReturnsResponse() {
        Instant timestamp = Instant.parse("2026-01-01T10:00:00Z");

        Service savedService = mock(Service.class);

        when(serviceRepository.findById(1L))
                .thenReturn(Optional.of(savedService));
        when(savedService.getId()).thenReturn(1L);
        when(savedService.getName()).thenReturn("Payments API");
        when(savedService.getDescription()).thenReturn("Payment processing");
        when(savedService.getCreatedAt()).thenReturn(timestamp);
        when(savedService.getUpdatedAt()).thenReturn(timestamp);

        ServiceResponse response =
                serviceManagementService.getServiceById(1L);

        assertEquals(1L, response.id());
        assertEquals("Payments API", response.name());
        assertEquals(timestamp, response.createdAt());
    }

    @Test
    void getServiceByIdThrowsWhenMissing() {
        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ServiceNotFoundException.class,
                () -> serviceManagementService.getServiceById(99L)
        );
    }

    @Test
    void getAllServicesReturnsResponses() {
        Service first = mock(Service.class);
        Service second = mock(Service.class);

        when(first.getId()).thenReturn(1L);
        when(first.getName()).thenReturn("Payments API");
        when(first.getDescription()).thenReturn("Payments");
        when(first.getCreatedAt()).thenReturn(Instant.EPOCH);
        when(first.getUpdatedAt()).thenReturn(Instant.EPOCH);

        when(second.getId()).thenReturn(2L);
        when(second.getName()).thenReturn("Identity API");
        when(second.getDescription()).thenReturn("Authentication");
        when(second.getCreatedAt()).thenReturn(Instant.EPOCH);
        when(second.getUpdatedAt()).thenReturn(Instant.EPOCH);

        when(serviceRepository.findAll()).thenReturn(List.of(first, second));

        List<ServiceResponse> responses =
                serviceManagementService.getAllServices();

        assertEquals(2, responses.size());
        assertEquals("Payments API", responses.get(0).name());
        assertEquals("Identity API", responses.get(1).name());
    }
}