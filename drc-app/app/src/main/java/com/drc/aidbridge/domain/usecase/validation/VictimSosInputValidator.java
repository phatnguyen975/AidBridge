package com.drc.aidbridge.domain.usecase.validation;

import javax.inject.Inject;

public class VictimSosInputValidator {

    @Inject
    public VictimSosInputValidator() {
    }

    public AuthValidationResult validateSelfSos(String fullName,
                                                int peopleCount,
                                                String severity,
                                                String note) {
        if (peopleCount <= 0) {
            return AuthValidationResult.invalid(
                AuthValidationResult.Field.PEOPLE_COUNT,
                "So nguoi can lon hon 0."
            );
        }

        if (severity == null || severity.trim().isEmpty()) {
            return AuthValidationResult.invalid(
                AuthValidationResult.Field.SEVERITY,
                "Vui long chon muc do khan cap."
            );
        }

        return AuthValidationResult.valid();
    }

    public AuthValidationResult validateRelativeSos(String relativeName,
                                                    String relativeAddress,
                                                    String severity) {
        if (relativeName == null || relativeName.trim().isEmpty()) {
            return AuthValidationResult.invalid(
                AuthValidationResult.Field.NAME,
                "Vui long nhap ho ten nguoi than can ho tro."
            );
        }

        if (relativeAddress == null || relativeAddress.trim().isEmpty()) {
            return AuthValidationResult.invalid(
                AuthValidationResult.Field.ADDRESS,
                "Vui long nhap dia chi hoac mo ta vi tri nguoi than."
            );
        }

        if (severity == null || severity.trim().isEmpty()) {
            return AuthValidationResult.invalid(
                AuthValidationResult.Field.SEVERITY,
                "Vui long chon muc do khan cap."
            );
        }

        return AuthValidationResult.valid();
    }
}
