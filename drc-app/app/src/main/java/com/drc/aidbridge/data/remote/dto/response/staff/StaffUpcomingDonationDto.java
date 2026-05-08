package com.drc.aidbridge.data.remote.dto.response.staff;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class StaffUpcomingDonationDto {

    @Nullable
    @SerializedName(value = "id", alternate = {"donationId"})
    private String id;

    @Nullable
    @SerializedName(value = "donationCode", alternate = {"code"})
    private String donationCode;

    @Nullable
    @SerializedName(value = "name", alternate = {"donorName", "sponsorName"})
    private String name;

    @Nullable
    @SerializedName(value = "phoneNumber", alternate = {"phone", "donorPhone", "sponsorPhone"})
    private String phoneNumber;

    @Nullable
    public String getId() {
        return id;
    }

    @Nullable
    public String getDonationCode() {
        return donationCode;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getPhoneNumber() {
        return phoneNumber;
    }
}
