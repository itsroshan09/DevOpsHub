package com.roshan.devopshub.service;

import org.hibernate.exception.ConstraintViolationException;

final class ServiceConstraintViolationDetector {

    private static final String SERVICE_NAME_CONSTRAINT =
            "uq_services_name";

    private ServiceConstraintViolationDetector() {
    }

    static boolean isDuplicateServiceName(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof ConstraintViolationException violation) {
                return SERVICE_NAME_CONSTRAINT.equals(
                        violation.getConstraintName()
                );
            }

            current = current.getCause();
        }

        return false;
    }
}