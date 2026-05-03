package com.drc.aidbridge.domain.usecase.validation;

import com.drc.aidbridge.domain.model.sponsor.SponsorDonationRequest;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationItem;

import java.util.List;
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

        List<SponsorDonationItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            return ValidationResult.invalid(ValidationResult.Field.ITEMS, "Vui lòng thêm ít nhất một vật phẩm.");
        }

        for (SponsorDonationItem item : items) {
            if (item == null) {
                return ValidationResult.invalid(ValidationResult.Field.ITEMS, "Vật phẩm không hợp lệ.");
            }

            String itemCategoryId = item.getItemCategoryId();
            if (isBlank(itemCategoryId) || !isValidUuid(itemCategoryId)) {
                return ValidationResult.invalid(ValidationResult.Field.ITEMS, "Loại vật phẩm đã chọn không hợp lệ.");
            }
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
