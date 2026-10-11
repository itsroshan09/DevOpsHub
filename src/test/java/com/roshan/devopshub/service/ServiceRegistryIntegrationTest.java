package com.roshan.devopshub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionSystemException;


@SpringBootTest
@Transactional
class ServiceRegistryIntegrationTest {

    @Autowired
    private ServiceManagementService serviceManagementService;

    @Autowired
    private ServiceRepository serviceRepository;

    @Test
    void shouldPersistServiceAndPopulateTimestamps() {
        String name = "integration-service-" + UUID.randomUUID();

        ServiceResponse created = serviceManagementService.createService(
                new ServiceCreateRequest(name, "Integration test service")
        );

        assertNotNull(created.id());
        assertNotNull(created.createdAt());
        assertNotNull(created.updatedAt());
        assertEquals(name, created.name());

        Service persisted = serviceRepository.findById(created.id())
                .orElseThrow();

        assertEquals(name, persisted.getName());
        assertEquals("Integration test service", persisted.getDescription());
        assertNotNull(persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());
    }

    @Test
    void shouldRejectDuplicateServiceName() {
        String name = "duplicate-service-" + UUID.randomUUID();
        ServiceCreateRequest request =
                new ServiceCreateRequest(name, "Test service");

        serviceManagementService.createService(request);

        assertThrows(
                DuplicateServiceNameException.class,
                () -> serviceManagementService.createService(request)
        );

        assertEquals(
                1,
                serviceRepository.findAll().stream()
                        .filter(service -> name.equals(service.getName()))
                        .count()
        );
    }


    @Test
    void shouldEnforceUniqueServiceNameAtDatabaseLevel() {
        String name = "unique-constraint-" + UUID.randomUUID();

        serviceRepository.saveAndFlush(
                new Service(name, "Original service")
        );

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> serviceRepository.saveAndFlush(
                        new Service(name, "Duplicate service")
                )
        );

        Throwable current = exception;

        while (current != null) {
            if (current instanceof
                    org.hibernate.exception.ConstraintViolationException violation) {
                assertEquals(
                        "uq_services_name",
                        violation.getConstraintName()
                );
                return;
            }

            current = current.getCause();
        }

        throw new AssertionError(
                "Expected a Hibernate constraint violation in the exception chain"
        );
    }

}