package com.roshan.devopshub.service;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository
        extends JpaRepository<Service, Long> {

    boolean existsByName(String name);
}