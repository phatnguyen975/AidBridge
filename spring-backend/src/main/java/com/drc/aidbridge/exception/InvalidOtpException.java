package com.drc.aidbridge.exception;

/**
 * Exception thrown when OTP verification fails.
 */
public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException(String message) {
        super(message);
    }
}
