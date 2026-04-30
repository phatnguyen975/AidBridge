package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StaffInventory {

    private final StaffInventoryHub hub;
    private final List<StaffInventoryFilter> filters;
    private final List<StaffInventoryItem> items;
    private final long totalItems;

    public StaffInventory(@Nullable StaffInventoryHub hub,
                          @Nullable List<StaffInventoryFilter> filters,
                          @Nullable List<StaffInventoryItem> items,
                          long totalItems) {
        this.hub = hub;
        this.filters = immutableCopy(filters);
        this.items = immutableCopy(items);
        this.totalItems = Math.max(totalItems, 0L);
    }

    @Nullable
    public StaffInventoryHub getHub() {
        return hub;
    }

    @NonNull
    public List<StaffInventoryFilter> getFilters() {
        return filters;
    }

    @NonNull
    public List<StaffInventoryItem> getItems() {
        return items;
    }

    public long getTotalItems() {
        return totalItems;
    }

    private <T> List<T> immutableCopy(@Nullable List<T> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }
}
