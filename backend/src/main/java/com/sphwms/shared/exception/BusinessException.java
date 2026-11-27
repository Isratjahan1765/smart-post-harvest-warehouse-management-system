package com.sphwms.shared.exception;

/**
 * Thrown when a business rule is violated (e.g. insufficient stock,
 * duplicate phone number). Maps to HTTP 400 via GlobalExceptionHandler.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
