package com.drc.aidbridge.modules.shared.exception;

/**
 * Raised when a Cloudinary operation fails for reasons other than invalid input.
 */
public class CloudinaryOperationException extends RuntimeException {

    public CloudinaryOperationException(String message) {
        super(message);
    }

    public CloudinaryOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
