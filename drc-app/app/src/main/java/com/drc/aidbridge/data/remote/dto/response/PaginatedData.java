package com.drc.aidbridge.data.remote.dto.response;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class PaginatedData<T> {

    @SerializedName(value = "items", alternate = {"content", "records"})
    private List<T> items;

    @SerializedName(value = "page", alternate = {"currentPage"})
    private int page;

    @SerializedName(value = "size", alternate = {"pageSize"})
    private int size;

    @SerializedName(value = "total_pages", alternate = {"totalPages"})
    private int totalPages;

    @SerializedName(value = "total_items", alternate = {"totalItems"})
    private long totalItems;

    @SerializedName(value = "has_next", alternate = {"hasNext"})
    private boolean hasNext;

    public List<T> getItems() {
        return items != null ? items : Collections.emptyList();
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public boolean hasNext() {
        return hasNext;
    }
}