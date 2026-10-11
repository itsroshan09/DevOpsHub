package com.roshan.devopshub.service;

public class DuplicateServiceNameException extends RuntimeException {

    public DuplicateServiceNameException() {
        super("A service with this name already exists");
    }
}