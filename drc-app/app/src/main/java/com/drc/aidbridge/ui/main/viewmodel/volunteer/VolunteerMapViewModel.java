package com.drc.aidbridge.ui.main.viewmodel.volunteer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.RoutingRequestDto;
import com.drc.aidbridge.data.remote.dto.response.RoutingResponseDto;
import com.drc.aidbridge.domain.usecase.routing.CalculateRouteUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Handler;
import android.os.Looper;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VolunteerMapViewModel extends BaseViewModel {

    private final MutableLiveData<RoutingRequestDto> calculateRouteTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<RoutingResponseDto>> routeResult;

    private String selectedStrategy = "urgent_response";
    private boolean avoidDangerousZones;
    private boolean isNavigationActive;
    private boolean isManualStartPoint;
    private boolean awaitDevStartPin = true;
    private boolean isNetworkDropSimulated;
    private boolean isTopOverviewExpanded;

    @Nullable
    private GeoPoint startPoint;
    @Nullable
    private GeoPoint endPoint;
    @Nullable
    private GeoPoint currentPoint;
    @Nullable
    private String startAddress;
    @Nullable
    private String endAddress;
    @Nullable
    private Double lastDistanceMeters;
    @Nullable
    private Long lastDurationSeconds;
    @Nullable
    private String lastPolyline;

    private final List<GeoPoint> routePoints = new ArrayList<>();
    private final List<RoutingResponseDto.InstructionDto> instructions = new ArrayList<>();

    private final Handler simulationHandler = new Handler(Looper.getMainLooper());
    private Runnable simulationRunnable;
    private final MutableLiveData<GeoPoint> simulatedPoint = new MutableLiveData<>();
    private final MutableLiveData<Boolean> simulationState = new MutableLiveData<>(false);
    private boolean simulationRunning;
    private boolean simulationPausedByLifecycle;
    private int simulationPointIndex;
    private int lastSimulationPointBeforePause;  // Persist simulation progress
    private int cachedInstructionIndex = -1;  // Persist HUD instruction state
    private final List<RoutingRequestDto.DangerousZoneDto> cachedDangerousZones = new ArrayList<>();  // Persist dangerous zones

    @Inject
    public VolunteerMapViewModel(CalculateRouteUseCase calculateRouteUseCase) {
        routeResult = Transformations.switchMap(calculateRouteTrigger, calculateRouteUseCase::execute);
    }

    public LiveData<NetworkResultWrapper<RoutingResponseDto>> getRouteResult() {
        return routeResult;
    }

    public void calculateRoute(RoutingRequestDto requestDto) {
        calculateRouteTrigger.setValue(requestDto);
    }

    @NonNull
    public String getSelectedStrategy() {
        return selectedStrategy;
    }

    public void setSelectedStrategy(@NonNull String selectedStrategy) {
        this.selectedStrategy = selectedStrategy;
    }

    public boolean isAvoidDangerousZones() {
        return avoidDangerousZones;
    }

    public void setAvoidDangerousZones(boolean avoidDangerousZones) {
        this.avoidDangerousZones = avoidDangerousZones;
    }

    public boolean isNavigationActive() {
        return isNavigationActive;
    }

    public void setNavigationActive(boolean navigationActive) {
        isNavigationActive = navigationActive;
    }

    public boolean isManualStartPoint() {
        return isManualStartPoint;
    }

    public void setManualStartPoint(boolean manualStartPoint) {
        isManualStartPoint = manualStartPoint;
    }

    public boolean isAwaitDevStartPin() {
        return awaitDevStartPin;
    }

    public void setAwaitDevStartPin(boolean awaitDevStartPin) {
        this.awaitDevStartPin = awaitDevStartPin;
    }

    public boolean isNetworkDropSimulated() {
        return isNetworkDropSimulated;
    }

    public void setNetworkDropSimulated(boolean networkDropSimulated) {
        isNetworkDropSimulated = networkDropSimulated;
    }

    public boolean isTopOverviewExpanded() {
        return isTopOverviewExpanded;
    }

    public void setTopOverviewExpanded(boolean topOverviewExpanded) {
        isTopOverviewExpanded = topOverviewExpanded;
    }

    @Nullable
    public GeoPoint getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(@Nullable GeoPoint startPoint) {
        this.startPoint = startPoint;
    }

    @Nullable
    public GeoPoint getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(@Nullable GeoPoint endPoint) {
        this.endPoint = endPoint;
    }

    @Nullable
    public GeoPoint getCurrentPoint() {
        return currentPoint;
    }

    public void setCurrentPoint(@Nullable GeoPoint currentPoint) {
        this.currentPoint = currentPoint;
    }

    @Nullable
    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(@Nullable String startAddress) {
        this.startAddress = startAddress;
    }

    @Nullable
    public String getEndAddress() {
        return endAddress;
    }

    public void setEndAddress(@Nullable String endAddress) {
        this.endAddress = endAddress;
    }

    @Nullable
    public Double getLastDistanceMeters() {
        return lastDistanceMeters;
    }

    public void setLastDistanceMeters(@Nullable Double lastDistanceMeters) {
        this.lastDistanceMeters = lastDistanceMeters;
    }

    @Nullable
    public Long getLastDurationSeconds() {
        return lastDurationSeconds;
    }

    public void setLastDurationSeconds(@Nullable Long lastDurationSeconds) {
        this.lastDurationSeconds = lastDurationSeconds;
    }

    @Nullable
    public String getLastPolyline() {
        return lastPolyline;
    }

    public void setLastPolyline(@Nullable String lastPolyline) {
        this.lastPolyline = lastPolyline;
    }

    @NonNull
    public List<GeoPoint> getRoutePoints() {
        return new ArrayList<>(routePoints);
    }

    public void setRoutePoints(@Nullable List<GeoPoint> points) {
        routePoints.clear();
        if (points != null) {
            routePoints.addAll(points);
        }
    }

    @NonNull
    public List<RoutingResponseDto.InstructionDto> getInstructions() {
        return new ArrayList<>(instructions);
    }

    public void setInstructions(@Nullable List<RoutingResponseDto.InstructionDto> instructionList) {
        instructions.clear();
        if (instructionList != null) {
            instructions.addAll(instructionList);
        }
    }

    public LiveData<GeoPoint> getSimulatedPoint() {
        return simulatedPoint;
    }

    public LiveData<Boolean> getSimulationState() {
        return simulationState;
    }

    public boolean isSimulationRunning() {
        return simulationRunning;
    }

    public int getSimulationPointIndex() {
        return simulationPointIndex;
    }

    public int getLastSimulationPointBeforePause() {
        return lastSimulationPointBeforePause;
    }

    public void setLastSimulationPointBeforePause(int index) {
        this.lastSimulationPointBeforePause = index;
    }

    public int getCachedInstructionIndex() {
        return cachedInstructionIndex;
    }

    public void setCachedInstructionIndex(int index) {
        this.cachedInstructionIndex = index;
    }

    @NonNull
    public List<RoutingRequestDto.DangerousZoneDto> getCachedDangerousZones() {
        return new ArrayList<>(cachedDangerousZones);
    }

    public void setCachedDangerousZones(@Nullable List<RoutingRequestDto.DangerousZoneDto> zones) {
        cachedDangerousZones.clear();
        if (zones != null) {
            cachedDangerousZones.addAll(zones);
        }
    }

    public void startSimulationFromCurrentRoute() {
        if (routePoints.isEmpty() || simulationRunning) {
            return;
        }

        simulationPointIndex = 0;
        simulationPausedByLifecycle = false;
        startSimulationLoop();
    }

    public void pauseSimulationForBackground() {
        if (!simulationRunning) {
            return;
        }

        simulationPausedByLifecycle = true;
        lastSimulationPointBeforePause = simulationPointIndex;  // Save progress
        simulationRunning = false;
        simulationState.postValue(false);

        if (simulationRunnable != null) {
            simulationHandler.removeCallbacks(simulationRunnable);
            simulationRunnable = null;
        }
    }

    public void resumeSimulationIfNeeded() {
        if (!simulationPausedByLifecycle || routePoints.isEmpty() || simulationRunning) {
            return;
        }
        simulationPausedByLifecycle = false;
        // Restore simulation point from where it paused
        simulationPointIndex = Math.min(lastSimulationPointBeforePause, routePoints.size() - 1);
        startSimulationLoop();
    }

    private void startSimulationLoop() {
        simulationRunning = true;
        simulationState.postValue(true);

        simulationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!simulationRunning) {
                    return;
                }

                if (simulationPointIndex >= routePoints.size()) {
                    stopSimulation();
                    return;
                }

                GeoPoint point = routePoints.get(simulationPointIndex++);
                currentPoint = point;
                simulatedPoint.postValue(point);
                simulationHandler.postDelayed(this, 1000L);
            }
        };
        simulationHandler.post(simulationRunnable);
    }

    public void stopSimulation() {
        simulationRunning = false;
        simulationPausedByLifecycle = false;
        simulationState.postValue(false);

        if (simulationRunnable != null) {
            simulationHandler.removeCallbacks(simulationRunnable);
            simulationRunnable = null;
        }
    }

    public void clearRouteData() {
        lastPolyline = null;
        lastDistanceMeters = null;
        lastDurationSeconds = null;
        routePoints.clear();
        instructions.clear();
    }

    @Override
    protected void onCleared() {
        stopSimulation();
        super.onCleared();
    }
}
