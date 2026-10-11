package com.roshan.devopshub.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ServicePersistenceWriter {

    private final ServiceRepository serviceRepository;

    public ServicePersistenceWriter(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Transactional
    public Service save(Service service) {
        return serviceRepository.saveAndFlush(service);
    }
}