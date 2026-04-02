package com.drc.aidbridge.modules.shared.exception;

/**
 * Exception thrown when request validation fails or business rule is violated.
 * Maps to HTTP 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
