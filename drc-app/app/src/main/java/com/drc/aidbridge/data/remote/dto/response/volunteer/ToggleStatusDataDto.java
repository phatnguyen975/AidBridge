package com.drc.aidbridge.data.remote.dto.response.volunteer;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class ToggleStatusDataDto {

    @Nullable
    @SerializedName("profile")
    private ProfileDto profile;

    @Nullable
    public ProfileDto getProfile() {
        return profile;
    }

    public static class ProfileDto {

        @SerializedName("online")
        private boolean online;

        public boolean isOnline() {
            return online;
        }
    }
}
