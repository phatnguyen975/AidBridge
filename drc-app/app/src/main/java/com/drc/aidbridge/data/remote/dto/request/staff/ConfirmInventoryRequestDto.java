package com.drc.aidbridge.data.remote.dto.request.staff;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ConfirmInventoryRequestDto {

    @SerializedName("code")
    private final String code;

    @SerializedName("items")
    private final List<ConfirmInventoryItemRequestDto> items;

    @SerializedName("note")
    private final String note;

    public ConfirmInventoryRequestDto(String code,
                                      List<ConfirmInventoryItemRequestDto> items,
                                      String note) {
        this.code = code;
        this.items = items;
        this.note = note;
    }
}
