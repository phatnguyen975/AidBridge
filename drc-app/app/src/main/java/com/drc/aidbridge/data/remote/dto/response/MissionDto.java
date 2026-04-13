package com.drc.aidbridge.data.remote.dto.response;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class MissionDto {

    @SerializedName("id")
    private String id;

    @SerializedName("missionType")
    private String missionType;

    @SerializedName("status")
    private String status;

    @SerializedName("victimLat")
    private Double victimLat;

    @SerializedName("victimLng")
    private Double victimLng;

    @SerializedName("priorityScore")
    private BigDecimal priorityScore;

    @Nullable
    @SerializedName("comment")
    private String comment;

    @Nullable
    @SerializedName("sosRequest")
    private SosRequestBriefDto sosRequest;

    public String getId() {
        return id;
    }

    public String getMissionType() {
        return missionType;
    }

    public String getStatus() {
        return status;
    }

    public Double getVictimLat() {
        return victimLat;
    }

    public Double getVictimLng() {
        return victimLng;
    }

    public BigDecimal getPriorityScore() {
        return priorityScore;
    }

    @Nullable
    public String getComment() {
        return comment;
    }

    @Nullable
    public SosRequestBriefDto getSosRequest() {
        return sosRequest;
    }

    public static class SosRequestBriefDto {

        @Nullable
        @SerializedName("address")
        private String address;

        @Nullable
        @SerializedName("description")
        private String description;

        public String getAddress() {
            return address;
        }

        public String getDescription() {
            return description;
        }
    }
}
