package com.sphwms.shared.exception;

/**
 * Thrown when a requested resource does not exist in the database.
 * Maps to HTTP 404 via GlobalExceptionHandler.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
