package com.drc.aidbridge.domain.usecase.validation;

/**
 * Shared validation contract for all use cases.
 */
public final class ValidationResult {

    public enum Field {
        EMAIL,
        PASSWORD,
        CONFIRM_PASSWORD,
        NAME,
        PHONE,
        OTP,
        ROLE,
        ADDRESS,
        ITEMS,
        PEOPLE_COUNT,
        SEVERITY,
        NONE
    }

    private final boolean valid;
    private final String errorMessage;
    private final Field errorField;

    private ValidationResult(boolean valid, String errorMessage, Field errorField) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.errorField = errorField;
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, null, Field.NONE);
    }

    public static ValidationResult invalid(Field errorField, String message) {
        return new ValidationResult(false, message, errorField);
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Field getErrorField() {
        return errorField;
    }
}
