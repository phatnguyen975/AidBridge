package com.drc.aidbridge.data.remote.dto.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GeoJsonGeometryDto {
    @SerializedName("type")
    private String type;

    @SerializedName("coordinates")
    private List<List<List<Double>>> coordinates;

    public GeoJsonGeometryDto() {}

    public GeoJsonGeometryDto(String type, List<List<List<Double>>> coordinates) {
        this.type = type;
        this.coordinates = coordinates;
    }

    public String getType() { return type; }
    public List<List<List<Double>>> getCoordinates() { return coordinates; }
}
