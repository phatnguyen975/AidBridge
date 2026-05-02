package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class StaffInventoryFilter {

    public static final String TYPE_ALL = "ALL";
    public static final String TYPE_PARENT_CATEGORY = "PARENT_CATEGORY";

    private final String id;
    private final String name;
    private final String type;

    public StaffInventoryFilter(@Nullable String id, String name, String type) {
        this.id = safeNullableText(id);
        this.name = safeText(name);
        this.type = safeText(type);
    }

    @Nullable
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public boolean isAll() {
        return TYPE_ALL.equals(type);
    }

    private String safeNullableText(String value) {
        String safe = safeText(value);
        return safe.isEmpty() ? null : safe;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
