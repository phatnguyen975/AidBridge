package com.drc.aidbridge.modules.admin.internal.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DashboardItemCategory {
    WATER("Nước uống"),
    CLOTHING("Quần áo"),
    FOOD("Thực phẩm"),
    MEDICINE("Thuốc men"),
    OTHER("Nhu yếu phẩm khác");

    private final String label;
}
