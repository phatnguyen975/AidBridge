package com.drc.aidbridge.domain.usecase.validation;

import javax.inject.Inject;

public class VictimSosInputValidator {

    @Inject
    public VictimSosInputValidator() {
    }

    public ValidationResult validateSelfSos(String fullName,
                                                int peopleCount,
                                                String severity,
                                                String note) {
        if (peopleCount <= 0) {
            return ValidationResult.invalid(
                ValidationResult.Field.PEOPLE_COUNT,
                "Số người cần hỗ trợ phải lớn hơn 0."
            );
        }

        if (severity == null || severity.trim().isEmpty()) {
            return ValidationResult.invalid(
                ValidationResult.Field.SEVERITY,
                "Vui lòng chọn mức độ khẩn cấp."
            );
        }

        return ValidationResult.valid();
    }

    public ValidationResult validateRelativeSos(String relativeName,
                                                    String relativeAddress,
                                                    String severity) {
        if (relativeName == null || relativeName.trim().isEmpty()) {
            return ValidationResult.invalid(
                ValidationResult.Field.NAME,
                "Vui lòng nhập họ tên người thân cần hỗ trợ."
            );
        }

        if (relativeAddress == null || relativeAddress.trim().isEmpty()) {
            return ValidationResult.invalid(
                ValidationResult.Field.ADDRESS,
                "Vui lòng nhập địa chỉ hoặc mô tả vị trí người thân."
            );
        }

        if (severity == null || severity.trim().isEmpty()) {
            return ValidationResult.invalid(
                ValidationResult.Field.SEVERITY,
                "Vui lòng chọn mức độ khẩn cấp."
            );
        }

        return ValidationResult.valid();
    }
}
