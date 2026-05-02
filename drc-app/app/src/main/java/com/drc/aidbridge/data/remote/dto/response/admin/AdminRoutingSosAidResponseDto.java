package com.drc.aidbridge.data.remote.dto.response.admin;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AdminRoutingSosAidResponseDto {
    @SerializedName("sosRequests")
    private List<AdminSosRequestDto> sosRequests;

    @SerializedName("aidRequests")
    private List<AdminAidRequestDto> aidRequests;

    public List<AdminSosRequestDto> getSosRequests() { return sosRequests; }
    public List<AdminAidRequestDto> getAidRequests() { return aidRequests; }

    public static class AdminSosRequestDto {
        private String id;
        private Double lat;
        private Double lng;
        private String address;
        private String description;
        private String createdAt;
        private String status;

        public String getId() { return id; }
        public Double getLat() { return lat; }
        public Double getLng() { return lng; }
        public String getAddress() { return address; }
        public String getDescription() { return description; }
        public String getCreatedAt() { return createdAt; }
        public String getStatus() { return status; }
    }

    public static class AdminAidRequestDto {
        private String id;
        private Double lat;
        private Double lng;
        private String address;
        private String description;
        private String createdAt;
        private String status;

        public String getId() { return id; }
        public Double getLat() { return lat; }
        public Double getLng() { return lng; }
        public String getAddress() { return address; }
        public String getDescription() { return description; }
        public String getCreatedAt() { return createdAt; }
        public String getStatus() { return status; }
    }
}
