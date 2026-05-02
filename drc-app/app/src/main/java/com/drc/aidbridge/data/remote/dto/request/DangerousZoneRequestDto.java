package com.drc.aidbridge.data.remote.dto.request;

import com.drc.aidbridge.data.remote.dto.response.GeoJsonGeometryDto;
import com.google.gson.annotations.SerializedName;
import java.util.UUID;

public class DangerousZoneRequestDto {
    @SerializedName("name")
    private String name;

    @SerializedName("geometry")
    private GeoJsonGeometryDto geometry;

    @SerializedName("adminId")
    private UUID adminId;

    public DangerousZoneRequestDto(String name, GeoJsonGeometryDto geometry, UUID adminId) {
        this.name = name;
        this.geometry = geometry;
        this.adminId = adminId;
    }

    public String getName() { return name; }
    public GeoJsonGeometryDto getGeometry() { return geometry; }
    public UUID getAdminId() { return adminId; }
}
