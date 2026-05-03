package com.drc.aidbridge.ui.map.base;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.RoutingRequestDto;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.RoutingResponseDto;
import com.drc.aidbridge.data.remote.dto.response.DangerousZoneResponseDto;
import com.drc.aidbridge.data.remote.dto.response.hub.HubDto;
import com.drc.aidbridge.domain.repository.HubRepository;
import com.drc.aidbridge.domain.repository.RoutingRepository;
import com.drc.aidbridge.domain.usecase.routing.CalculateRouteUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BaseMapViewModel extends BaseViewModel {

    public static final String STRATEGY_URGENT = "urgent_response";
    public static final String STRATEGY_SAFE = "disaster_safe";
    public static final String STRATEGY_HEAVY_AID = "heavy_aid";
    public static final String STRATEGY_COMMUNITY = "community_delivery";
    public static final String STRATEGY_OFFROAD = "offroad_terrain";

    private static final String DEFAULT_HUB_SEARCH_ERROR_MESSAGE = "Khong tim thay Hub nao";
    private static final String DEFAULT_NETWORK_ERROR_MESSAGE = "Khong the ket noi den may chu";

    private String selectedStrategy = STRATEGY_URGENT;
    private boolean avoidDangerousZones = true;
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
    @Nullable
    private String lastRouteSource;

    private final List<GeoPoint> routePoints = new ArrayList<>();
    private final List<RoutingResponseDto.InstructionDto> instructions = new ArrayList<>();

    private final MutableLiveData<RoutingRequestDto> calculateRouteTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<RoutingResponseDto>> routeResult;

    private final MutableLiveData<NetworkResultWrapper<List<HubDto>>> hubSearchResult = new MutableLiveData<>();
    private final MutableLiveData<GeoPoint> hubSearchPingPoint = new MutableLiveData<>();

    private final Handler simulationHandler = new Handler(Looper.getMainLooper());
    private Runnable simulationRunnable;
    private final MutableLiveData<GeoPoint> simulatedPoint = new MutableLiveData<>();
    private final MutableLiveData<Boolean> simulationState = new MutableLiveData<>(false);
    private boolean simulationRunning;
    private boolean simulationPausedByLifecycle;
    private int simulationPointIndex;
    private int lastSimulationPointBeforePause;
    private int cachedInstructionIndex = -1;
    private final List<RoutingRequestDto.DangerousZoneDto> cachedDangerousZones = new ArrayList<>();
    private final MutableLiveData<NetworkResultWrapper<List<DangerousZoneResponseDto>>> serverDangerousZones = new MutableLiveData<>();

    protected final RoutingRepository routingRepository;
    protected final HubRepository hubRepository;

    protected BaseMapViewModel(@NonNull CalculateRouteUseCase calculateRouteUseCase,
            @NonNull HubRepository hubRepository,
            @NonNull RoutingRepository routingRepository) {
        this.hubRepository = hubRepository;
        this.routingRepository = routingRepository;
        routeResult = Transformations.switchMap(calculateRouteTrigger, calculateRouteUseCase::execute);

        // Initial fetch of dangerous zones
        fetchDangerousZones();
    }

    public LiveData<NetworkResultWrapper<List<DangerousZoneResponseDto>>> getServerDangerousZones() {
        return serverDangerousZones;
    }

    public void fetchDangerousZones() {
        android.util.Log.d("BaseMapViewModel", "fetchDangerousZones: Triggered");
        routingRepository.getDangerousZones().observeForever(result -> {
            android.util.Log.d("BaseMapViewModel", "fetchDangerousZones: Received result. Status: " + (result != null ? result.getClass().getSimpleName() : "null"));
            if (result != null) {
                serverDangerousZones.postValue(result);
                if (result.isSuccess() && result.getData() != null) {
                    android.util.Log.d("BaseMapViewModel", "fetchDangerousZones: Success. Data size: " + result.getData().size());
                    // Update cached zones for routing
                    List<RoutingRequestDto.DangerousZoneDto> mappedZones = new ArrayList<>();
                    for (DangerousZoneResponseDto zone : result.getData()) {
                        android.util.Log.d("BaseMapViewModel", "Mapping zone: " + zone.getName());
                        mappedZones.add(new RoutingRequestDto.DangerousZoneDto(
                                zone.getId().toString(),
                                0,
                                new RoutingRequestDto.GeometryDto(
                                        zone.getGeometry().getType(),
                                        zone.getGeometry().getCoordinates())));
                    }
                    android.util.Log.d("BaseMapViewModel", "fetchDangerousZones: Successfully mapped " + mappedZones.size() + " zones.");
                    setCachedDangerousZones(mappedZones);
                }
            }
        });
    }

    public LiveData<NetworkResultWrapper<RoutingResponseDto>> getRouteResult() {
        return routeResult;
    }

    public void calculateRoute(@NonNull RoutingRequestDto requestDto) {
        android.util.Log.d("BaseMapViewModel", "Calculating route with " + 
            (requestDto.getDangerousZones() != null ? requestDto.getDangerousZones().size() : 0) + 
            " dangerous zones. Strategy: " + requestDto.getStrategy());
        calculateRouteTrigger.setValue(requestDto);
    }

    public LiveData<NetworkResultWrapper<List<HubDto>>> getHubSearchResult() {
        return hubSearchResult;
    }

    public LiveData<GeoPoint> getHubSearchPingPoint() {
        return hubSearchPingPoint;
    }

    public void setHubSearchPingPoint(@Nullable GeoPoint point) {
        hubSearchPingPoint.setValue(point);
    }

    public void searchHubsNearLocation(@NonNull String status, double lat, double lon, double radiusKm) {
        hubSearchResult.setValue(NetworkResultWrapper.loading());
        hubRepository.getHubsNearLocation(status, lat, lon, radiusKm * 1000d)
                .enqueue(new Callback<BaseResponse<List<HubDto>>>() {
                    @Override
                    public void onResponse(@NonNull Call<BaseResponse<List<HubDto>>> call,
                            @NonNull Response<BaseResponse<List<HubDto>>> response) {
                        BaseResponse<List<HubDto>> body = response.body();
                        if (response.isSuccessful() && body != null && body.isSuccess() && body.getData() != null) {
                            hubSearchResult.postValue(NetworkResultWrapper.success(body.getData()));
                            return;
                        }

                        String errorMessage = resolveHubSearchErrorMessage(body);
                        hubSearchResult.postValue(NetworkResultWrapper.error(errorMessage));
                    }

                    @Override
                    public void onFailure(@NonNull Call<BaseResponse<List<HubDto>>> call,
                            @NonNull Throwable throwable) {
                        String message = throwable.getMessage();
                        if (message == null || message.trim().isEmpty()) {
                            message = DEFAULT_NETWORK_ERROR_MESSAGE;
                        }
                        hubSearchResult.postValue(NetworkResultWrapper.error(message));
                    }
                });
    }

    @NonNull
    public RoutingRequestDto createRoutingRequest(@NonNull GeoPoint start,
            @NonNull GeoPoint end,
            @Nullable List<RoutingRequestDto.DangerousZoneDto> overrideZones) {
        List<RoutingRequestDto.DangerousZoneDto> dangerousZones = Collections.emptyList();
        if (avoidDangerousZones) {
            dangerousZones = (overrideZones == null || overrideZones.isEmpty())
                    ? buildDefaultDangerousZones()
                    : new ArrayList<>(overrideZones);
        }

        RoutingRequestDto request = new RoutingRequestDto(
                start.getLatitude(),
                start.getLongitude(),
                end.getLatitude(),
                end.getLongitude(),
                selectedStrategy,
                dangerousZones);
        
        android.util.Log.d("BaseMapViewModel", "Created RoutingRequest with " + dangerousZones.size() + " zones. Avoid=" + avoidDangerousZones);
        return request;
    }

    @NonNull
    public RoutingRequestDto createRoutingRequest(@NonNull GeoPoint start, @NonNull GeoPoint end) {
        return createRoutingRequest(start, end, null);
    }

    @NonNull
    public List<RoutingRequestDto.DangerousZoneDto> buildDefaultDangerousZones() {
        // Now returns the zones fetched from the server and cached in the ViewModel
        return getCachedDangerousZones();
    }

    private List<List<List<Double>>> createPolygonCoordinates(double[][] lonLatPoints) {
        List<List<List<Double>>> polygonCoordinates = new ArrayList<>();
        List<List<Double>> ring = new ArrayList<>();
        for (double[] lonLat : lonLatPoints) {
            ring.add(Arrays.asList(lonLat[0], lonLat[1]));
        }
        polygonCoordinates.add(ring);
        return polygonCoordinates;
    }

    @NonNull
    private String resolveHubSearchErrorMessage(@Nullable BaseResponse<List<HubDto>> body) {
        if (body != null && body.getMessage() != null && !body.getMessage().trim().isEmpty()) {
            return body.getMessage().trim();
        }
        return DEFAULT_HUB_SEARCH_ERROR_MESSAGE;
    }

    @NonNull
    public String getSelectedStrategy() {
        return selectedStrategy;
    }

    public void setSelectedStrategy(@NonNull String selectedStrategy) {
        if (isSupportedStrategy(selectedStrategy)) {
            this.selectedStrategy = selectedStrategy;
            return;
        }
        this.selectedStrategy = STRATEGY_URGENT;
    }

    private boolean isSupportedStrategy(@Nullable String strategy) {
        if (strategy == null) {
            return false;
        }
        return STRATEGY_URGENT.equals(strategy)
                || STRATEGY_SAFE.equals(strategy)
                || STRATEGY_HEAVY_AID.equals(strategy)
                || STRATEGY_COMMUNITY.equals(strategy)
                || STRATEGY_OFFROAD.equals(strategy);
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

    @Nullable
    public String getLastRouteSource() {
        return lastRouteSource;
    }

    public void setLastRouteSource(@Nullable String lastRouteSource) {
        this.lastRouteSource = lastRouteSource;
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
        if (zones == null) {
            android.util.Log.w("BaseMapViewModel", "setCachedDangerousZones: Received null list!");
            return;
        }
        cachedDangerousZones.clear();
        cachedDangerousZones.addAll(zones);
        android.util.Log.d("BaseMapViewModel", "Cache updated. New size: " + cachedDangerousZones.size());
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
        lastSimulationPointBeforePause = simulationPointIndex;
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
        lastRouteSource = null;
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