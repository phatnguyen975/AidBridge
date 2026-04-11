package com.drc.aidbridge.domain.usecase.validation;

import com.drc.aidbridge.domain.model.victim.VictimReliefRequest;
import com.drc.aidbridge.domain.model.victim.VictimRequestedItem;

import java.util.List;

import javax.inject.Inject;

public class VictimSupplyInputValidator {

    @Inject
    public VictimSupplyInputValidator() {
    }

    public ValidationResult validateReliefRequest(VictimReliefRequest request) {
        if (request == null) {
            return ValidationResult.invalid(
                ValidationResult.Field.NONE,
                "Dữ liệu yêu cầu không hợp lệ."
            );
        }

        List<VictimRequestedItem> requestedItems = request.getRequestedItems();
        if (requestedItems == null || requestedItems.isEmpty()) {
            return ValidationResult.invalid(
                ValidationResult.Field.ITEMS,
                "Vui lòng chọn ít nhất một vật phẩm cần hỗ trợ."
            );
        }

        return ValidationResult.valid();
    }
}
