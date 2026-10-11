package com.roshan.devopshub.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final ServicePersistenceWriter servicePersistenceWriter;

    public ServiceManagementService(
            ServiceRepository serviceRepository,
            ServicePersistenceWriter servicePersistenceWriter
    ) {
        this.serviceRepository = serviceRepository;
        this.servicePersistenceWriter = servicePersistenceWriter;
    }

    public ServiceResponse createService(ServiceCreateRequest request) {
        String normalizedName = request.name().trim();

        // Fast, friendly check for ordinary duplicate requests.
        if (serviceRepository.existsByName(normalizedName)) {
            throw new DuplicateServiceNameException();
        }

        Service service = new Service(
                normalizedName,
                request.description()
        );

        Service savedService;

        try {
            // The writer manages the database write transaction.
            savedService = servicePersistenceWriter.save(service);
        } catch (DataIntegrityViolationException exception) {
            // Translate only the specific service-name constraint violation.
            if (ServiceConstraintViolationDetector
                    .isDuplicateServiceName(exception)) {
                throw new DuplicateServiceNameException();
            }

            // Preserve unrelated database errors.
            throw exception;
        }

        return toResponse(savedService);
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> getAllServices() {
        return serviceRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ServiceNotFoundException(id));

        return toResponse(service);
    }

    private ServiceResponse toResponse(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
}
