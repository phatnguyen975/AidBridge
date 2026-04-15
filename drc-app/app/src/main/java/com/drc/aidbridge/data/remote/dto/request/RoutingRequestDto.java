package com.drc.aidbridge.data.remote.dto.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RoutingRequestDto carries payload for POST /routing/calculate.
 */
public class RoutingRequestDto {

    @SerializedName("startLat")
    private final double startLat;

    @SerializedName("startLon")
    private final double startLon;

    @SerializedName("endLat")
    private final double endLat;

    @SerializedName("endLon")
    private final double endLon;

    @SerializedName("strategy")
    private final String strategy;

    @SerializedName("dangerousZones")
    private final List<DangerousZoneDto> dangerousZones;

    public RoutingRequestDto(double startLat,
                             double startLon,
                             double endLat,
                             double endLon,
                             @NonNull String strategy,
                             @Nullable List<DangerousZoneDto> dangerousZones) {
        this.startLat = startLat;
        this.startLon = startLon;
        this.endLat = endLat;
        this.endLon = endLon;
        this.strategy = strategy;
        this.dangerousZones = dangerousZones == null
                ? Collections.emptyList()
                : new ArrayList<>(dangerousZones);
    }

    public double getStartLat() {
        return startLat;
    }

    public double getStartLon() {
        return startLon;
    }

    public double getEndLat() {
        return endLat;
    }

    public double getEndLon() {
        return endLon;
    }

    @NonNull
    public String getStrategy() {
        return strategy;
    }

    @NonNull
    public List<DangerousZoneDto> getDangerousZones() {
        return dangerousZones;
    }

    public static class DangerousZoneDto {

        @SerializedName("name")
        private final String name;

        @SerializedName("priority")
        private final int priority;

        @SerializedName("geometry")
        private final GeometryDto geometry;

        public DangerousZoneDto(@NonNull String name, int priority, @NonNull GeometryDto geometry) {
            this.name = name;
            this.priority = priority;
            this.geometry = geometry;
        }

        @NonNull
        public String getName() {
            return name;
        }

        public int getPriority() {
            return priority;
        }

        @NonNull
        public GeometryDto getGeometry() {
            return geometry;
        }
    }

    public static class GeometryDto {

        @SerializedName("type")
        private final String type;

        @SerializedName("coordinates")
        private final List<List<List<Double>>> coordinates;

        public GeometryDto(@NonNull String type, @NonNull List<List<List<Double>>> coordinates) {
            this.type = type;
            this.coordinates = coordinates;
        }

        @NonNull
        public String getType() {
            return type;
        }

        @NonNull
        public List<List<List<Double>>> getCoordinates() {
            return coordinates;
        }
    }
}
