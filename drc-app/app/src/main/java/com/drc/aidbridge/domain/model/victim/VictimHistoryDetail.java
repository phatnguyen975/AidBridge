package com.drc.aidbridge.domain.model.victim;

import java.util.Collections;
import java.util.List;

public class VictimHistoryDetail {

    private final String requestId;
    private final String type;
    private final String status;
    private final String statusType;
    private final String dateTime;
    private final String location;
    private final String condition;

    private final Integer peopleCount;
    private final Integer numberAdult;
    private final Integer numberElderly;
    private final Integer numberChildren;

    private final String noteFullName;
    private final String notePhoneNumber;
    private final String noteHealthDetail;

    private final List<VictimHistoryAidItemDetail> requestedItems;

    public VictimHistoryDetail(String requestId,
                               String type,
                               String status,
                               String statusType,
                               String dateTime,
                               String location,
                               String condition,
                               Integer peopleCount,
                               Integer numberAdult,
                               Integer numberElderly,
                               Integer numberChildren,
                               String noteFullName,
                               String notePhoneNumber,
                               String noteHealthDetail,
                               List<VictimHistoryAidItemDetail> requestedItems) {
        this.requestId = requestId != null ? requestId.trim() : "";
        this.type = type != null ? type.trim() : "";
        this.status = status != null ? status.trim() : "";
        this.statusType = statusType != null ? statusType.trim() : "";
        this.dateTime = dateTime != null ? dateTime.trim() : "";
        this.location = location != null ? location.trim() : "";
        this.condition = condition != null ? condition.trim() : "";
        this.peopleCount = peopleCount;
        this.numberAdult = numberAdult;
        this.numberElderly = numberElderly;
        this.numberChildren = numberChildren;
        this.noteFullName = noteFullName != null ? noteFullName.trim() : "";
        this.notePhoneNumber = notePhoneNumber != null ? notePhoneNumber.trim() : "";
        this.noteHealthDetail = noteHealthDetail != null ? noteHealthDetail.trim() : "";
        this.requestedItems = requestedItems != null ? requestedItems : Collections.emptyList();
    }

    public String getRequestId() {
        return requestId;
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

    public String getDateTime() {
        return dateTime;
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

    public List<VictimHistoryAidItemDetail> getRequestedItems() {
        return requestedItems;
    }
}
