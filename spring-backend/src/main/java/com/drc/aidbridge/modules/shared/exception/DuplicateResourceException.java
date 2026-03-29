package com.drc.aidbridge.modules.shared.exception;

/**
 * Exception thrown when a duplicate resource is detected.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
