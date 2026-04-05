package com.drc.aidbridge.data.remote.dto.response.volunteer;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class VolunteerProfileDataDto {

    @Nullable
    @SerializedName("user")
    private VolunteerUserDto user;

    @Nullable
    @SerializedName("profile")
    private ProfileDto profile;

    @Nullable
    public VolunteerUserDto getUser() {
        return user;
    }

    @Nullable
    public ProfileDto getProfile() {
        return profile;
    }

    public static class ProfileDto {

        @SerializedName("online")
        private boolean online;

        @SerializedName("totalTasksCompleted")
        private int totalTasksCompleted;

        public boolean isOnline() {
            return online;
        }

        public int getTotalTasksCompleted() {
            return totalTasksCompleted;
        }
    }
}
