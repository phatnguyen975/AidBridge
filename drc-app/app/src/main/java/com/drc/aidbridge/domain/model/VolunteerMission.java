package com.drc.aidbridge.domain.model;

import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.time.Instant;

public class VolunteerMission {

    private final String id;
    private final String missionType;
    private final String status;
    @Nullable
    private final String codeName;
    @Nullable
    private final Double victimLat;
    @Nullable
    private final Double victimLng;
    @Nullable
    private final BigDecimal priorityScore;
    @Nullable
    private final String address;
    @Nullable
    private final String note;
    @Nullable
    private final String comment;
    @Nullable
    private final String dispatchAttemptId;
    @Nullable
    private final Instant expiresAt;

    public VolunteerMission(String id,
                            String missionType,
                            String status,
                            @Nullable String codeName,
                            @Nullable Double victimLat,
                            @Nullable Double victimLng,
                            @Nullable BigDecimal priorityScore,
                            @Nullable String address,
                            @Nullable String note,
                            @Nullable String comment,
                            @Nullable String dispatchAttemptId,
                            @Nullable Instant expiresAt) {
        this.id = id;
        this.missionType = missionType;
        this.status = status;
        this.codeName = codeName;
        this.victimLat = victimLat;
        this.victimLng = victimLng;
        this.priorityScore = priorityScore;
        this.address = address;
        this.note = note;
        this.comment = comment;
        this.dispatchAttemptId = dispatchAttemptId;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public String getMissionType() {
        return missionType;
    }

    public String getStatus() {
        return status;
    }

    @Nullable
    public String getCodeName() {
        return codeName;
    }

    @Nullable
    public Double getVictimLat() {
        return victimLat;
    }

    @Nullable
    public Double getVictimLng() {
        return victimLng;
    }

    @Nullable
    public BigDecimal getPriorityScore() {
        return priorityScore;
    }

    @Nullable
    public String getAddress() {
        return address;
    }

    @Nullable
    public String getNote() {
        return note;
    }

    @Nullable
    public String getComment() {
        return comment;
    }

    @Nullable
    public String getDispatchAttemptId() {
        return dispatchAttemptId;
    }

    @Nullable
    public Instant getExpiresAt() {
        return expiresAt;
    }

    public VolunteerMission withDispatchContext(@Nullable String nextDispatchAttemptId, @Nullable Instant nextExpiresAt) {
        return new VolunteerMission(
                id,
                missionType,
                status,
                codeName,
                victimLat,
                victimLng,
                priorityScore,
                address,
                note,
                comment,
                nextDispatchAttemptId,
                nextExpiresAt
        );
    }
}
