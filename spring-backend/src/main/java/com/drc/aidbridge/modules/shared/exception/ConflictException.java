package com.drc.aidbridge.modules.shared.exception;

/**
 * Exception thrown when a valid request conflicts with the current resource state.
 * Maps to HTTP 409 Conflict.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
