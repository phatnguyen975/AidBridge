package com.drc.aidbridge.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "victim_history_cache")
public class VictimHistoryEntity {

    @PrimaryKey
    @NonNull
    public String cacheKey;

    @NonNull
    public String requestId;

    @NonNull
    public String title;

    @NonNull
    public String status;

    @NonNull
    public String statusType;

    @NonNull
    public String dateTime;

    @NonNull
    public String location;

    @NonNull
    public String type;

    @NonNull
    public String detail;

    @NonNull
    public String timeRange;

    public int page;

    public int orderInPage;

    public boolean hasNextPage;

    public long cachedAt;

    public VictimHistoryEntity(@NonNull String cacheKey,
                               @NonNull String requestId,
                               @NonNull String title,
                               @NonNull String status,
                               @NonNull String statusType,
                               @NonNull String dateTime,
                               @NonNull String location,
                               @NonNull String type,
                               @NonNull String detail,
                               @NonNull String timeRange,
                               int page,
                               int orderInPage,
                               boolean hasNextPage,
                               long cachedAt) {
        this.cacheKey = cacheKey;
        this.requestId = requestId;
        this.title = title;
        this.status = status;
        this.statusType = statusType;
        this.dateTime = dateTime;
        this.location = location;
        this.type = type;
        this.detail = detail;
        this.timeRange = timeRange;
        this.page = page;
        this.orderInPage = orderInPage;
        this.hasNextPage = hasNextPage;
        this.cachedAt = cachedAt;
    }
}
