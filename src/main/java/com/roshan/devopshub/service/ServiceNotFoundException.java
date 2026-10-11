package com.roshan.devopshub.service;

public class ServiceNotFoundException extends RuntimeException {

    public ServiceNotFoundException(Long id) {
        super("Service not found with id: " + id);
    }
}