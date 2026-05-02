package com.drc.aidbridge.data.remote.dto.request.staff;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ConfirmInboundInventoryRequestDto {

    @SerializedName("donationId")
    private final String donationId;

    @SerializedName("code")
    private final String code;

    @SerializedName("items")
    private final List<ConfirmInboundInventoryItemRequestDto> items;

    @SerializedName("generalNote")
    private final String generalNote;

    public ConfirmInboundInventoryRequestDto(String donationId,
                                            String code,
                                            List<ConfirmInboundInventoryItemRequestDto> items,
                                            String generalNote) {
        this.donationId = donationId;
        this.code = code;
        this.items = items;
        this.generalNote = generalNote;
    }
}
