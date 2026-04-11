package com.drc.aidbridge.domain.model.victim;

public class VictimHistoryItem {

    private final String requestId;
    private final String title;
    private final String status;
    private final String statusType;
    private final String dateTime;
    private final String location;
    private final String type;
    private final String detail;

    public VictimHistoryItem(String requestId,
                             String title,
                             String status,
                             String statusType,
                             String dateTime,
                             String location,
                             String type,
                             String detail) {
        this.requestId = requestId != null ? requestId.trim() : "";
        this.title = title != null ? title.trim() : "";
        this.status = status != null ? status.trim() : "";
        this.statusType = statusType != null ? statusType.trim() : "";
        this.dateTime = dateTime != null ? dateTime.trim() : "";
        this.location = location != null ? location.trim() : "";
        this.type = type != null ? type.trim() : "";
        this.detail = detail != null ? detail.trim() : "";
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
