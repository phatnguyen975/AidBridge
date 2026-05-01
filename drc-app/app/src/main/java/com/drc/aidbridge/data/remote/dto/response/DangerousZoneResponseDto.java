package com.drc.aidbridge.data.remote.dto.response;

import com.google.gson.annotations.SerializedName;
import java.util.UUID;

public class DangerousZoneResponseDto {
    @SerializedName("id")
    private UUID id;

    @SerializedName("name")
    private String name;

    @SerializedName("geometry")
    private GeoJsonGeometryDto geometry;

    @SerializedName("adminId")
    private UUID adminId;

    public DangerousZoneResponseDto() {}

    public UUID getId() { return id; }
    public String getName() { return name; }
    public GeoJsonGeometryDto getGeometry() { return geometry; }
    public UUID getAdminId() { return adminId; }
}
