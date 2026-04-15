package com.drc.aidbridge.data.remote.dto.response;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * RoutingResponseDto maps route metadata and turn-by-turn guidance.
 */
public class RoutingResponseDto {

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
