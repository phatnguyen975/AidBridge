package com.drc.aidbridge.data.remote.dto.response.staff;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class StaffUpcomingDeliveryMissionDto {

    @Nullable
    @SerializedName(value = "id", alternate = {"missionId"})
    private String id;

    @Nullable
    @SerializedName(value = "missionCode", alternate = {"code"})
    private String missionCode;

    @Nullable
    @SerializedName(value = "volunteerName", alternate = {"name"})
    private String volunteerName;

    @Nullable
    @SerializedName(value = "volunteerPhone", alternate = {"phone", "phoneNumber"})
    private String volunteerPhone;

    @Nullable
    public String getId() {
        return id;
    }

    @Nullable
    public String getMissionCode() {
        return missionCode;
    }

    @Nullable
    public String getVolunteerName() {
        return volunteerName;
    }

    @Nullable
    public String getVolunteerPhone() {
        return volunteerPhone;
    }
}
