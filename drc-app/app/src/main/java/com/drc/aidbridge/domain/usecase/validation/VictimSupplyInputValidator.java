package com.drc.aidbridge.domain.usecase.validation;

import com.drc.aidbridge.data.remote.dto.supply.ReliefRequestDto;
import com.drc.aidbridge.data.remote.dto.supply.RequestedItemDto;

import java.util.List;

import javax.inject.Inject;

public class VictimSupplyInputValidator {

    @Inject
    public VictimSupplyInputValidator() {
    }

    public AuthValidationResult validateReliefRequest(ReliefRequestDto requestDto) {
        if (requestDto == null) {
            return AuthValidationResult.invalid(
                AuthValidationResult.Field.NONE,
                "Du lieu yeu cau tiep te khong hop le."
            );
        }

        List<RequestedItemDto> requestedItems = requestDto.getRequestedItems();
        if (requestedItems == null || requestedItems.isEmpty()) {
            return AuthValidationResult.invalid(
                AuthValidationResult.Field.ITEMS,
                "Vui long chon it nhat mot vat pham tiep te."
            );
        }

        return AuthValidationResult.valid();
    }
}
