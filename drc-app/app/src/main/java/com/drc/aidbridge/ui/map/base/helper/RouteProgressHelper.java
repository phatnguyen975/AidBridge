package com.drc.aidbridge.ui.map.base.helper;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.dto.response.RoutingResponseDto;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouteProgressHelper {

    private static final double INSTRUCTION_UPDATE_LOCATION_THRESHOLD_METERS = 10d;
    private static final int INSTRUCTION_ROUTE_POINT_WINDOW = 140;
    private static final double INSTRUCTION_ROUTE_FULL_SCAN_THRESHOLD_METERS = 120d;

    private final List<GeoPoint> routePoints = new ArrayList<>();
    private final List<Double> routeProgressMeters = new ArrayList<>();
    private List<RoutingResponseDto.InstructionDto> currentInstructions = Collections.emptyList();

    private int cachedInstructionIndex = -1;
    private int cachedNearestRoutePointIndex = -1;
    @Nullable
    private GeoPoint lastInstructionIndexLocation;

    public void setRouteData(@NonNull List<GeoPoint> points,
                             @Nullable List<RoutingResponseDto.InstructionDto> instructions) {
        this.routePoints.clear();
        this.routePoints.addAll(points);
        this.currentInstructions = instructions != null ? instructions : Collections.emptyList();
        this.cachedInstructionIndex = -1;
        this.cachedNearestRoutePointIndex = -1;
        this.lastInstructionIndexLocation = null;
        rebuildRouteProgressCache();
    }

    public void clear() {
        routePoints.clear();
        routeProgressMeters.clear();
        currentInstructions = Collections.emptyList();
        cachedInstructionIndex = -1;
        cachedNearestRoutePointIndex = -1;
        lastInstructionIndexLocation = null;
    }

    public void setCachedInstructionIndex(int index) {
        this.cachedInstructionIndex = index;
    }

    public int getCachedInstructionIndex() {
        return cachedInstructionIndex;
    }

    public List<GeoPoint> getRoutePoints() {
        return routePoints;
    }

    public List<RoutingResponseDto.InstructionDto> getCurrentInstructions() {
        return currentInstructions;
    }

    public void rebuildRouteProgressCache() {
        routeProgressMeters.clear();
        if (routePoints.isEmpty()) {
            return;
        }

        double cumulativeDistance = 0d;
        routeProgressMeters.add(0d);

        for (int i = 1; i < routePoints.size(); i++) {
            cumulativeDistance += distanceMeters(routePoints.get(i - 1), routePoints.get(i));
            routeProgressMeters.add(cumulativeDistance);
        }
    }

    public int resolveInstructionIndex(@Nullable GeoPoint currentPoint, @Nullable GeoPoint startPoint) {
        if (currentInstructions.isEmpty() || routePoints.isEmpty()) {
            return 0;
        }

        GeoPoint reference = currentPoint != null ? currentPoint : startPoint;
        if (reference == null) {
            return 0;
        }

        if (lastInstructionIndexLocation != null
                && distanceMeters(reference, lastInstructionIndexLocation) < INSTRUCTION_UPDATE_LOCATION_THRESHOLD_METERS
                && cachedInstructionIndex >= 0
                && cachedInstructionIndex < currentInstructions.size()) {
            return cachedInstructionIndex;
        }

        lastInstructionIndexLocation = new GeoPoint(reference.getLatitude(), reference.getLongitude());

        int nearestPointIndex = findNearestRoutePointIndex(reference);
        if (routePoints.size() <= 1) {
            return 0;
        }

        int mappedByDistance = mapInstructionIndexByRouteDistance(nearestPointIndex);
        if (mappedByDistance >= 0) {
            if (cachedInstructionIndex >= 0 && mappedByDistance < cachedInstructionIndex) {
                return cachedInstructionIndex;
            }
            cachedInstructionIndex = mappedByDistance;
            return mappedByDistance;
        }

        double ratio = nearestPointIndex / (double) (routePoints.size() - 1);
        int mappedIndex = (int) Math.floor(ratio * currentInstructions.size());
        if (mappedIndex >= currentInstructions.size()) {
            mappedIndex = currentInstructions.size() - 1;
        }
        if (cachedInstructionIndex >= 0 && mappedIndex < cachedInstructionIndex) {
            mappedIndex = cachedInstructionIndex;
        }
        
        cachedInstructionIndex = Math.max(mappedIndex, 0);
        return cachedInstructionIndex;
    }

    public double resolveRemainingDistanceForInstruction(int instructionIndex,
                                                          @Nullable GeoPoint currentPoint,
                                                          @Nullable GeoPoint startPoint) {
        if (instructionIndex < 0 || instructionIndex >= currentInstructions.size()) {
            return 0d;
        }

        RoutingResponseDto.InstructionDto instruction = currentInstructions.get(instructionIndex);
        double instructionDistance = instruction != null && instruction.getDistance() != null
                ? Math.max(0d, instruction.getDistance())
                : 0d;

        if (instructionDistance <= 1d || routeProgressMeters.isEmpty()) {
            return instructionDistance;
        }

        GeoPoint reference = currentPoint != null ? currentPoint : startPoint;
        if (reference == null) {
            return instructionDistance;
        }

        int nearestPointIndex = findNearestRoutePointIndex(reference);
        if (nearestPointIndex < 0 || nearestPointIndex >= routeProgressMeters.size()) {
            return instructionDistance;
        }

        double routeTotalDistance = routeProgressMeters.get(routeProgressMeters.size() - 1);
        if (routeTotalDistance <= 1d) {
            return instructionDistance;
        }

        double instructionTotalDistance = 0d;
        for (RoutingResponseDto.InstructionDto item : currentInstructions) {
            if (item == null || item.getDistance() == null) {
                continue;
            }
            instructionTotalDistance += Math.max(0d, item.getDistance());
        }

        if (instructionTotalDistance <= 1d) {
            return instructionTotalDistance;
        }

        double routeProgressDistance = routeProgressMeters.get(nearestPointIndex);
        double normalizedInstructionDistance = (routeProgressDistance / routeTotalDistance) * instructionTotalDistance;

        double instructionStartDistance = 0d;
        for (int i = 0; i < instructionIndex; i++) {
            RoutingResponseDto.InstructionDto previous = currentInstructions.get(i);
            if (previous == null || previous.getDistance() == null) {
                continue;
            }
            instructionStartDistance += Math.max(0d, previous.getDistance());
        }

        double traveledWithinInstruction = Math.max(0d, normalizedInstructionDistance - instructionStartDistance);
        double remainingDistance = instructionDistance - traveledWithinInstruction;

        if (remainingDistance < 0d) {
            return 0d;
        }
        return Math.min(remainingDistance, instructionDistance);
    }

    public int findNextDistinctInstructionIndex(int currentIndex, @NonNull Context context) {
        if (currentIndex < 0 || currentIndex >= currentInstructions.size()) {
            return -1;
        }

        String currentLabel = buildCommandWithRoad(currentInstructions.get(currentIndex), context).trim();
        for (int i = currentIndex + 1; i < currentInstructions.size(); i++) {
            String nextLabel = buildCommandWithRoad(currentInstructions.get(i), context).trim();
            if (!nextLabel.equalsIgnoreCase(currentLabel)) {
                return i;
            }
        }
        return -1;
    }

    public int findNearestRoutePointIndex(@NonNull GeoPoint reference) {
        if (routePoints.isEmpty()) {
            return 0;
        }

        int size = routePoints.size();
        int searchStart = 0;
        int searchEnd = size;

        if (cachedNearestRoutePointIndex >= 0 && cachedNearestRoutePointIndex < size) {
            searchStart = Math.max(0, cachedNearestRoutePointIndex - INSTRUCTION_ROUTE_POINT_WINDOW);
            searchEnd = Math.min(size, cachedNearestRoutePointIndex + INSTRUCTION_ROUTE_POINT_WINDOW + 1);
        }

        int nearestIndex = searchNearestRoutePointInRange(reference, searchStart, searchEnd);
        double nearestDistance = distanceMeters(reference, routePoints.get(nearestIndex));

        if ((searchStart > 0 || searchEnd < size)
                && nearestDistance > INSTRUCTION_ROUTE_FULL_SCAN_THRESHOLD_METERS) {
            nearestIndex = searchNearestRoutePointInRange(reference, 0, size);
        }

        cachedNearestRoutePointIndex = nearestIndex;
        return nearestIndex;
    }

    private int searchNearestRoutePointInRange(@NonNull GeoPoint reference, int startInclusive, int endExclusive) {
        int safeStart = Math.max(0, startInclusive);
        int safeEnd = Math.min(routePoints.size(), endExclusive);

        if (safeStart >= safeEnd) {
            return 0;
        }

        int nearestIndex = safeStart;
        double minDistance = Double.MAX_VALUE;
        for (int i = safeStart; i < safeEnd; i++) {
            double distance = distanceMeters(reference, routePoints.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }

    private int mapInstructionIndexByRouteDistance(int nearestPointIndex) {
        if (currentInstructions.isEmpty()
                || routeProgressMeters.isEmpty()
                || nearestPointIndex < 0
                || nearestPointIndex >= routeProgressMeters.size()) {
            return -1;
        }

        double routeTotalDistance = routeProgressMeters.get(routeProgressMeters.size() - 1);
        if (routeTotalDistance <= 1d) {
            return -1;
        }

        double instructionTotalDistance = 0d;
        for (RoutingResponseDto.InstructionDto instruction : currentInstructions) {
            if (instruction == null || instruction.getDistance() == null) {
                continue;
            }
            instructionTotalDistance += Math.max(0d, instruction.getDistance());
        }

        if (instructionTotalDistance <= 1d) {
            return -1;
        }

        double routeProgressDistance = routeProgressMeters.get(nearestPointIndex);
        double normalizedInstructionDistance = (routeProgressDistance / routeTotalDistance) * instructionTotalDistance;

        double accumulated = 0d;
        for (int i = 0; i < currentInstructions.size(); i++) {
            RoutingResponseDto.InstructionDto instruction = currentInstructions.get(i);
            if (instruction != null && instruction.getDistance() != null) {
                accumulated += Math.max(0d, instruction.getDistance());
            }
            if (accumulated >= normalizedInstructionDistance) {
                return i;
            }
        }

        return currentInstructions.size() - 1;
    }

    public String buildInstructionLabel(@Nullable RoutingResponseDto.InstructionDto instruction,
                                        double overrideDistanceMeters,
                                        @NonNull Context context) {
        if (instruction == null) {
            return context.getString(R.string.base_map_instruction_continue);
        }

        String command = buildCommandWithRoad(instruction, context);

        double distance = overrideDistanceMeters >= 0d
                ? overrideDistanceMeters
                : instruction.getDistance() != null ? instruction.getDistance() : 0d;

        return command + " - " + formatDistance(distance, context);
    }

    public String buildCommandWithRoad(@Nullable RoutingResponseDto.InstructionDto instruction,
                                       @NonNull Context context) {
        if (instruction == null) {
            return context.getString(R.string.base_map_instruction_continue);
        }

        String command = instruction.getCommand();
        if (command == null || command.trim().isEmpty()) {
            command = context.getString(R.string.base_map_instruction_continue);
        } else {
            command = command.trim();
        }

        String road = instruction.getName();
        if (road == null || road.trim().isEmpty()) {
            return command;
        }

        return context.getString(R.string.base_map_instruction_with_road, command, road.trim());
    }

    public String formatDistance(double distanceMeters, @NonNull Context context) {
        if (distanceMeters >= 1000d) {
            return context.getString(R.string.base_map_distance_km, distanceMeters / 1000d);
        }
        return context.getString(R.string.base_map_distance_m, Math.round(distanceMeters));
    }

    private double distanceMeters(@NonNull GeoPoint from, @NonNull GeoPoint to) {
        android.location.Location fromLocation = new android.location.Location("from");
        fromLocation.setLatitude(from.getLatitude());
        fromLocation.setLongitude(from.getLongitude());

        android.location.Location toLocation = new android.location.Location("to");
        toLocation.setLatitude(to.getLatitude());
        toLocation.setLongitude(to.getLongitude());

        return fromLocation.distanceTo(toLocation);
    }
}
