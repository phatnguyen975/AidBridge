package com.drc.aidbridge.modules.shared.exception;

/**
 * Exception thrown when OTP verification fails.
 */
public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException(String message) {
        super(message);
    }
}
