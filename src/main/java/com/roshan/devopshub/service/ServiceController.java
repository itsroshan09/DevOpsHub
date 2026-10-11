
package com.roshan.devopshub.service;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceManagementService serviceManagementService;

    public ServiceController(
            ServiceManagementService serviceManagementService) {
        this.serviceManagementService = serviceManagementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse createService(
            @Valid @RequestBody ServiceCreateRequest request) {
        return serviceManagementService.createService(request);
    }

    @GetMapping
    public List<ServiceResponse> getAllServices() {
        return serviceManagementService.getAllServices();
    }

    @GetMapping("/{id}")
    public ServiceResponse getServiceById(@PathVariable Long id) {
        return serviceManagementService.getServiceById(id);
    }
}
