package com.drc.aidbridge.data.remote.dto.response.volunteer;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class MissionHistoryFullItemDto {
    @SerializedName("id")
    private String id;

    @Nullable
    @SerializedName("sosRequestId")
    private String sosRequestId;

    @Nullable
    @SerializedName("aidRequestId")
    private String aidRequestId;

    @SerializedName("volunteerId")
    private String volunteerId;

    @Nullable
    @SerializedName("hubId")
    private String hubId;

    @Nullable
    @SerializedName("codeName")
    private String codeName;

    @SerializedName("missionType")
    private String missionType;

    @SerializedName("status")
    private String status;

    @Nullable
    @SerializedName("qrCodeToken")
    private String qrCodeToken;

    @Nullable
    @SerializedName("priorityScore")
    private Double priorityScore;

    @Nullable
    @SerializedName("victimLat")
    private Double victimLat;

    @Nullable
    @SerializedName("victimLng")
    private Double victimLng;

    @Nullable
    @SerializedName("acceptedAt")
    private String acceptedAt;

    @Nullable
    @SerializedName("pickedUpAt")
    private String pickedUpAt;

    @Nullable
    @SerializedName("startedAt")
    private String startedAt;

    @Nullable
    @SerializedName("completedAt")
    private String completedAt;

    @Nullable
    @SerializedName("cancelledAt")
    private String cancelledAt;

    @Nullable
    @SerializedName("cancellationReason")
    private String cancellationReason;

    @Nullable
    @SerializedName("address")
    private String address;

    @Nullable
    @SerializedName("confirmationImageUrl")
    private String confirmationImageUrl;

    @Nullable
    @SerializedName("imageUrl")
    private String imageUrl;

    @Nullable
    @SerializedName("comment")
    private String comment;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    @Nullable
    @SerializedName("radiusKm")
    private Double radiusKm;

    @Nullable
    @SerializedName("description")
    private String description;

    public String getId() { return id; }
    @Nullable public String getSosRequestId() { return sosRequestId; }
    @Nullable public String getAidRequestId() { return aidRequestId; }
    public String getVolunteerId() { return volunteerId; }
    @Nullable public String getHubId() { return hubId; }
    @Nullable public String getCodeName() { return codeName; }
    public String getMissionType() { return missionType; }
    public String getStatus() { return status; }
    @Nullable public String getQrCodeToken() { return qrCodeToken; }
    @Nullable public Double getPriorityScore() { return priorityScore; }
    @Nullable public Double getVictimLat() { return victimLat; }
    @Nullable public Double getVictimLng() { return victimLng; }
    @Nullable public String getAcceptedAt() { return acceptedAt; }
    @Nullable public String getPickedUpAt() { return pickedUpAt; }
    @Nullable public String getStartedAt() { return startedAt; }
    @Nullable public String getCompletedAt() { return completedAt; }
    @Nullable public String getCancelledAt() { return cancelledAt; }
    @Nullable public String getCancellationReason() { return cancellationReason; }
    @Nullable public String getAddress() { return address; }
    @Nullable public String getConfirmationImageUrl() { return confirmationImageUrl; }
    @Nullable public String getImageUrl() { return imageUrl; }
    @Nullable public String getComment() { return comment; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    @Nullable public Double getRadiusKm() { return radiusKm; }
    @Nullable public String getDescription() { return description; }

    @Nullable
    @SerializedName("sosRequestDetail")
    private SosRequestDetail sosRequestDetail;

    @Nullable
    @SerializedName("aidRequestDetail")
    private AidRequestDetail aidRequestDetail;

    @Nullable public SosRequestDetail getSosRequestDetail() { return sosRequestDetail; }
    @Nullable public AidRequestDetail getAidRequestDetail() { return aidRequestDetail; }

    public static class SosRequestDetail {
        @SerializedName("id") private String id;
        @SerializedName("requesterId") private String requesterId;
        @SerializedName("lat") private Double lat;
        @SerializedName("lng") private Double lng;
        @SerializedName("address") private String address;
        @SerializedName("description") private String description;
        @SerializedName("peopleCount") private Integer peopleCount;
        @SerializedName("urgencyLevel") private String urgencyLevel;
        @SerializedName("status") private String status;
        @SerializedName("imageUrl") private String imageUrl;
        @SerializedName("createdAt") private String createdAt;
        @SerializedName("updatedAt") private String updatedAt;

        public String getId() { return id; }
        public String getRequesterId() { return requesterId; }
        public Double getLat() { return lat; }
        public Double getLng() { return lng; }
        public String getAddress() { return address; }
        public String getDescription() { return description; }
        public Integer getPeopleCount() { return peopleCount; }
        public String getUrgencyLevel() { return urgencyLevel; }
        public String getStatus() { return status; }
        public String getImageUrl() { return imageUrl; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
    }

    public static class AidRequestDetail {
        @SerializedName("id") private String id;
        @SerializedName("requesterId") private String requesterId;
        @SerializedName("status") private String status;
        @SerializedName("lat") private Double lat;
        @SerializedName("lng") private Double lng;
        @SerializedName("address") private String address;
        @SerializedName("description") private String description;
        @SerializedName("numberAdult") private Integer numberAdult;
        @SerializedName("numberElderly") private Integer numberElderly;
        @SerializedName("numberChildren") private Integer numberChildren;
        @SerializedName("createdAt") private String createdAt;
        @SerializedName("updatedAt") private String updatedAt;

        public String getId() { return id; }
        public String getRequesterId() { return requesterId; }
        public String getStatus() { return status; }
        public Double getLat() { return lat; }
        public Double getLng() { return lng; }
        public String getAddress() { return address; }
        public String getDescription() { return description; }
        public Integer getNumberAdult() { return numberAdult; }
        public Integer getNumberElderly() { return numberElderly; }
        public Integer getNumberChildren() { return numberChildren; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
    }
}
