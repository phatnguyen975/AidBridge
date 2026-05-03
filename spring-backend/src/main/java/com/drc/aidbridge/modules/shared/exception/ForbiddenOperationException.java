package com.drc.aidbridge.modules.shared.exception;

/**
 * Exception thrown when the authenticated user cannot operate on the target resource.
 * Maps to HTTP 403 Forbidden.
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
