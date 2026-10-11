
package com.roshan.devopshub.auth;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("An account with this email already exists");
    }
}
