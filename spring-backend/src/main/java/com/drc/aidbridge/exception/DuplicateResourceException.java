package com.drc.aidbridge.exception;

/**
 * Exception thrown when a duplicate resource is detected.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
