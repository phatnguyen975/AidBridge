package com.drc.aidbridge.modules.shared.exception;

/**
 * Exception thrown when an operation is attempted on a mission in an invalid
 * state.
 */
public class InvalidMissionStateException extends RuntimeException {
    public InvalidMissionStateException(String message) {
        super(message);
    }

    public InvalidMissionStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
