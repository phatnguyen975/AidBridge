package com.drc.aidbridge.data.remote.dto.response.victim;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class HistoryDetailResponse {

    @SerializedName(value = "id", alternate = {"request_id", "requestId"})
    private String id;

    @SerializedName(value = "type", alternate = {"request_type", "requestType"})
    private String type;

    @SerializedName(value = "status", alternate = {"status_label", "statusLabel"})
    private String status;

    @SerializedName(value = "statusType", alternate = {"status_type"})
    private String statusType;

    @SerializedName(value = "createdAt", alternate = {"created_at", "requestTime"})
    private String createdAt;

    @SerializedName(value = "location", alternate = {"address"})
    private String location;

    @SerializedName(value = "condition", alternate = {"urgency", "urgencyLabel"})
    private String condition;

    @SerializedName(value = "peopleCount", alternate = {"people_count"})
    private Integer peopleCount;

    @SerializedName(value = "numberAdult", alternate = {"number_adult"})
    private Integer numberAdult;

    @SerializedName(value = "numberElderly", alternate = {"number_elderly"})
    private Integer numberElderly;

    @SerializedName(value = "numberChildren", alternate = {"number_children"})
    private Integer numberChildren;

    @SerializedName(value = "noteFullName", alternate = {"note_full_name"})
    private String noteFullName;

    @SerializedName(value = "notePhoneNumber", alternate = {"note_phone_number"})
    private String notePhoneNumber;

    @SerializedName(value = "noteHealthDetail", alternate = {"note_health_detail"})
    private String noteHealthDetail;

    @SerializedName(value = "requestedItems", alternate = {"requested_items", "items"})
    private List<HistoryDetailItemResponse> requestedItems;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusType() {
        return statusType;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getLocation() {
        return location;
    }

    public String getCondition() {
        return condition;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public Integer getNumberAdult() {
        return numberAdult;
    }

    public Integer getNumberElderly() {
        return numberElderly;
    }

    public Integer getNumberChildren() {
        return numberChildren;
    }

    public String getNoteFullName() {
        return noteFullName;
    }

    public String getNotePhoneNumber() {
        return notePhoneNumber;
    }

    public String getNoteHealthDetail() {
        return noteHealthDetail;
    }

    public List<HistoryDetailItemResponse> getRequestedItems() {
        return requestedItems;
    }
}
