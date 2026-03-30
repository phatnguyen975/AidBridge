package com.drc.aidbridge.modules.shared.exception;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
