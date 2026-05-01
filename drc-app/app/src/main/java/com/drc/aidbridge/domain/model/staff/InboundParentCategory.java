package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class InboundParentCategory {

    private final String parentCategoryId;
    private final String parentCategoryName;
    private final String unit;
    private final List<InboundSubCategory> availableSubCategories;

    public InboundParentCategory(String parentCategoryId,
                                 String parentCategoryName,
                                 String unit,
                                 List<InboundSubCategory> availableSubCategories) {
        this.parentCategoryId = safeText(parentCategoryId);
        this.parentCategoryName = safeText(parentCategoryName);
        this.unit = safeText(unit);
        this.availableSubCategories = availableSubCategories != null
                ? new ArrayList<>(availableSubCategories)
                : new ArrayList<>();
    }

    @NonNull
    public String getParentCategoryId() {
        return parentCategoryId;
    }

    @NonNull
    public String getParentCategoryName() {
        return parentCategoryName;
    }

    @NonNull
    public String getUnit() {
        return unit;
    }

    @NonNull
    public List<InboundSubCategory> getAvailableSubCategories() {
        return availableSubCategories;
    }

    public void addSubCategory(InboundSubCategory subCategory) {
        if (subCategory != null) {
            availableSubCategories.add(subCategory);
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
