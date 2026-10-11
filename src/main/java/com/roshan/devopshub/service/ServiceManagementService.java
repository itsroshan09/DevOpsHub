
package com.roshan.devopshub.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ServiceManagementService {

    private final ServiceRepository serviceRepository;

    public ServiceManagementService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Transactional
    public ServiceResponse createService(ServiceCreateRequest request) {
        String normalizedName = request.name().trim();

        if (serviceRepository.existsByName(normalizedName)) {
            throw new DuplicateServiceNameException();
        }

        Service service = new Service(
                normalizedName,
                request.description()
        );

        Service savedService = serviceRepository.save(service);

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
