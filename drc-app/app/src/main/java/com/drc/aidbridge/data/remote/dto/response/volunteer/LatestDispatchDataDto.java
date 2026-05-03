package com.drc.aidbridge.data.remote.dto.response.volunteer;

import com.google.gson.annotations.SerializedName;

public class LatestDispatchDataDto {
    @SerializedName("id")
    private String id;

    @SerializedName("missionId")
    private String missionId;

    @SerializedName("volunteerId")
    private String volunteerId;

    @SerializedName("dispatchType")
    private String dispatchType;

    @SerializedName("batchNumber")
    private int batchNumber;

    @SerializedName("radiusKm")
    private double radiusKm;

    @SerializedName("response")
    private String response;

    public String getId() { return id; }
    public String getMissionId() { return missionId; }
    public String getVolunteerId() { return volunteerId; }
    public String getDispatchType() { return dispatchType; }
    public int getBatchNumber() { return batchNumber; }
    public double getRadiusKm() { return radiusKm; }
    public String getResponse() { return response; }
}
