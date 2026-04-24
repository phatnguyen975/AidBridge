package com.drc.aidbridge.domain.usecase.validation;

import com.drc.aidbridge.domain.model.sponsor.SponsorDonationRequest;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationItem;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

            if (isBlank(item.getItemName())) {
                return ValidationResult.invalid(ValidationResult.Field.ITEMS, "Vui lòng nhập tên vật phẩm.");
            }

            if (item.getQuantity() <= 0) {
                return ValidationResult.invalid(ValidationResult.Field.ITEMS, "Số lượng mỗi vật phẩm phải lớn hơn 0.");
            }

            if (isBlank(item.getUnit())) {
                return ValidationResult.invalid(ValidationResult.Field.ITEMS, "Vui lòng nhập đơn vị tính cho vật phẩm.");
            }

            String itemCategoryId = item.getItemCategoryId();
            if (!isBlank(itemCategoryId) && !isValidUuid(itemCategoryId)) {
                return ValidationResult.invalid(ValidationResult.Field.ITEMS, "itemCategoryId phải đúng định dạng UUID hoặc để trống.");
            }

            String expiryDate = item.getExpiryDate();
            if (!isBlank(expiryDate) && !isValidIsoDate(expiryDate)) {
                return ValidationResult.invalid(ValidationResult.Field.ITEMS, "Hạn sử dụng phải theo định dạng yyyy-MM-dd.");
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

    private boolean isValidIsoDate(String raw) {
        try {
            LocalDate.parse(raw.trim());
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }
}
