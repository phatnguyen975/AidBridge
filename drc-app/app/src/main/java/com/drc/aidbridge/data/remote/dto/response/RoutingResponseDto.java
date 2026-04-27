package com.drc.aidbridge.data.remote.dto.response;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * RoutingResponseDto maps route metadata and turn-by-turn guidance.
 */
public class RoutingResponseDto {

    public static final String ROUTE_SOURCE_ONLINE = "online";
    public static final String ROUTE_SOURCE_OFFLINE = "offline";

    @SerializedName("distance")
    private Double distance;

    @SerializedName("duration")
    private Long duration;

    @Nullable
    @SerializedName("polyline")
    private String polyline;

    @SerializedName("timestamp")
    private Long timestamp;

    @Nullable
    @SerializedName("instructions")
    private List<InstructionDto> instructions;

    @Nullable
    @SerializedName("routeSource")
    private String routeSource;

    public RoutingResponseDto() {
    }

    public RoutingResponseDto(@Nullable Double distance,
                              @Nullable Long duration,
                              @Nullable String polyline,
                              @Nullable Long timestamp,
                              @Nullable List<InstructionDto> instructions,
                              @Nullable String routeSource) {
        this.distance = distance;
        this.duration = duration;
        this.polyline = polyline;
        this.timestamp = timestamp;
        this.instructions = instructions;
        this.routeSource = routeSource;
    }

    @Nullable
    public Double getDistance() {
        return distance;
    }

    @Nullable
    public Long getDuration() {
        return duration;
    }

    @Nullable
    public String getPolyline() {
        return polyline;
    }

    @Nullable
    public Long getTimestamp() {
        return timestamp;
    }

    @Nullable
    public List<InstructionDto> getInstructions() {
        return instructions;
    }

    @Nullable
    public String getRouteSource() {
        return routeSource;
    }

    public void setRouteSource(@Nullable String routeSource) {
        this.routeSource = routeSource;
    }

    public static class InstructionDto {

        @SerializedName("turnType")
        private Integer turnType;

        @Nullable
        @SerializedName("name")
        private String name;

        @SerializedName("distance")
        private Double distance;

        @SerializedName("time")
        private Long time;

        @Nullable
        @SerializedName("command")
        private String command;

        public InstructionDto() {
        }

        public InstructionDto(@Nullable Integer turnType,
                              @Nullable String name,
                              @Nullable Double distance,
                              @Nullable Long time,
                              @Nullable String command) {
            this.turnType = turnType;
            this.name = name;
            this.distance = distance;
            this.time = time;
            this.command = command;
        }

        @Nullable
        public Integer getTurnType() {
            return turnType;
        }

        @Nullable
        public String getName() {
            return name;
        }

        @Nullable
        public Double getDistance() {
            return distance;
        }

        @Nullable
        public Long getTime() {
            return time;
        }

        @Nullable
        public String getCommand() {
            return command;
        }
    }
}
