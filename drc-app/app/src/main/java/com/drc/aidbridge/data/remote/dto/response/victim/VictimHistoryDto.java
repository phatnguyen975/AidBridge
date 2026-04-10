package com.drc.aidbridge.data.remote.dto.response.victim;

import com.google.gson.annotations.SerializedName;

public class VictimHistoryDto {

    @SerializedName(value = "id", alternate = {"requestId", "request_id", "code", "requestCode"})
    private String requestId;

    @SerializedName(value = "title", alternate = {"requestTitle", "request_title"})
    private String title;

    @SerializedName(value = "status", alternate = {"statusLabel", "status_label"})
    private String status;

    @SerializedName(value = "statusType", alternate = {"status_type", "state"})
    private String statusType;

    @SerializedName(value = "createdAt", alternate = {"created_at", "timestamp", "requestTime"})
    private String dateTime;

    @SerializedName(value = "location", alternate = {"address", "fullAddress", "full_address"})
    private String location;

    @SerializedName(value = "type", alternate = {"requestType", "request_type"})
    private String type;

    @SerializedName(value = "note", alternate = {"description", "detail", "details"})
    private String detail;

    public VictimHistoryDto() {
    }

    public VictimHistoryDto(String requestId,
                            String title,
                            String status,
                            String statusType,
                            String dateTime,
                            String location,
                            String type,
                            String detail) {
        this.requestId = requestId;
        this.title = title;
        this.status = status;
        this.statusType = statusType;
        this.dateTime = dateTime;
        this.location = location;
        this.type = type;
        this.detail = detail;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusType() {
        return statusType;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getLocation() {
        return location;
    }

    public String getType() {
        return type;
    }

    public String getDetail() {
        return detail;
    }
}