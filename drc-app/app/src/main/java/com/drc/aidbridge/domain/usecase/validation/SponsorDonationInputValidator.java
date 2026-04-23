package com.drc.aidbridge.domain.usecase.validation;

import com.drc.aidbridge.domain.model.sponsor.SponsorDonationRequest;

import java.util.UUID;

import javax.inject.Inject;

public class SponsorDonationInputValidator {

    @Inject
    public SponsorDonationInputValidator() {
    }

    public ValidationResult validateDonationRequest(SponsorDonationRequest request) {
        if (request == null) {
            return ValidationResult.invalid(ValidationResult.Field.NONE, "Dữ liệu đóng góp không hợp lệ.");
        }

        if (isBlank(request.getHubId())) {
            return ValidationResult.invalid(ValidationResult.Field.NONE, "Hiện chưa có trạm tiếp nhận khả dụng.");
        }

        if (!isValidUuid(request.getHubId())) {
            return ValidationResult.invalid(ValidationResult.Field.NONE, "Hub không hợp lệ. Vui lòng chọn lại trạm tiếp nhận.");
        }

        if (isBlank(request.getItemName())) {
            return ValidationResult.invalid(ValidationResult.Field.ITEMS, "Vui lòng nhập tên vật phẩm.");
        }

        if (request.getQuantity() <= 0) {
            return ValidationResult.invalid(ValidationResult.Field.ITEMS, "Số lượng phải lớn hơn 0.");
        }

        if (isBlank(request.getUnit())) {
            return ValidationResult.invalid(ValidationResult.Field.ITEMS, "Vui lòng nhập đơn vị tính.");
        }

        return ValidationResult.valid();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isValidUuid(String raw) {
        if (isBlank(raw)) {
            return false;
        }

        try {
            UUID.fromString(raw.trim());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
