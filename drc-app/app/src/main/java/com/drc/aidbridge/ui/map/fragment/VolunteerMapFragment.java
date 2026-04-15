package com.drc.aidbridge.ui.map.fragment;

import android.Manifest;
import android.content.Intent;
import android.graphics.Paint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.BuildConfig;
import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.dto.request.RoutingRequestDto;
import com.drc.aidbridge.data.remote.dto.response.RoutingResponseDto;
import com.drc.aidbridge.databinding.FragmentMapVolunteerBinding;
import com.drc.aidbridge.domain.model.VolunteerMission;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerMapViewModel;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerTaskViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerMapFragment extends BaseFragment<FragmentMapVolunteerBinding> {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 7024;
    private static final double DEFAULT_END_LAT = 11.0;
    private static final double DEFAULT_END_LON = 106.8;
    private static final double ROUTING_MIN_LON = 102.126801;
    private static final double ROUTING_MAX_LON = 114.333212;
    private static final double ROUTING_MIN_LAT = 7.8917885;
    private static final double ROUTING_MAX_LAT = 23.3996861;
    private static final long ROUTE_REQUEST_DEBOUNCE_MS = 1500L;
    private static final double ROUTE_SMOOTHING_SEGMENT_METERS = 16d;
    private static final int ROUTE_SMOOTHING_MAX_STEPS = 12;
    private static final double INSTRUCTION_UPDATE_LOCATION_THRESHOLD_METERS = 10d;
    private static final int INSTRUCTION_ROUTE_POINT_WINDOW = 140;
    private static final double INSTRUCTION_ROUTE_FULL_SCAN_THRESHOLD_METERS = 120d;

    private static final String STRATEGY_URGENT = "urgent_response";
    private static final String STRATEGY_SAFE = "disaster_safe";
    private static final String STRATEGY_HEAVY_AID = "heavy_aid";
    private static final String STRATEGY_COMMUNITY = "community_delivery";
    private static final String STRATEGY_OFFROAD = "offroad_terrain";

    private static final long DEV_TAP_WINDOW_MS = 1500L;

    private VolunteerMapViewModel volunteerMapViewModel;
    private VolunteerTaskViewModel volunteerTaskViewModel;

    private MapView mapView;
    private Marker startMarker;
    private Marker endMarker;
    private Marker currentMarker;
    private Polyline routePolylineCasing;
    private Polyline routePolyline;
    private final List<Polygon> dangerousZoneOverlays = new ArrayList<>();

    @Nullable
    private GeoPoint lastStartMarkerPoint;
    @Nullable
    private GeoPoint lastEndMarkerPoint;
    @Nullable
    private GeoPoint lastCurrentMarkerPoint;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private ExecutorService geocodeExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private GeoPoint startPoint;
    private GeoPoint endPoint;
    private GeoPoint currentPoint;

    private final List<GeoPoint> routePoints = new ArrayList<>();
    private final List<Double> routeProgressMeters = new ArrayList<>();
    private List<RoutingResponseDto.InstructionDto> currentInstructions = Collections.emptyList();
    private List<RoutingRequestDto.DangerousZoneDto> currentDangerousZones = Collections.emptyList();  // Track current zones

    private String selectedStrategy = STRATEGY_URGENT;
    private boolean avoidDangerousZones;
    private boolean isNavigationActive;
    private boolean isManualStartPoint;
    private boolean awaitDevStartPin = true;
    private boolean isNetworkDropSimulated;
    private boolean isTopOverviewExpanded = false;
    private boolean isMapScreenActive;
    private boolean shouldRefreshAutoStartOnNextFix = true;
    private boolean hasShownOutOfBoundsWarning;
    private boolean isRoutingLoading;
    private long lastRouteRequestAtMs;
    private long lastAvatarTapAt;
    private int avatarTapCount;
    private long lastMarkerUpdateAtMs;
    private static final long MARKER_UPDATE_THROTTLE_MS = 300L;
    private boolean isMapBaseInitialized;  // Track if map base is setup
    private boolean isMapResumeScheduled;
    private boolean hasCenteredOnInitialGps;
    private boolean pendingNavigationAfterRouteCalculation;
    private boolean isTopOverviewAutoHiddenForRoute;
    private int defaultTopOverviewPeekHeightPx;
    private int minTopOverviewPeekHeightPx;
    @Nullable
    private BottomSheetBehavior<View> topOverviewBottomSheetBehavior;
    private int cachedInstructionIndex = -1;
    private int cachedNearestRoutePointIndex = -1;
    @Nullable
    private GeoPoint lastInstructionIndexLocation;

    // Point selection mode
    private enum PointSelectionMode { START, END, NONE }
    private PointSelectionMode pointSelectionMode = PointSelectionMode.NONE;

    @Nullable
    @Override
    protected FragmentMapVolunteerBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentMapVolunteerBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        volunteerMapViewModel = new ViewModelProvider(requireActivity()).get(VolunteerMapViewModel.class);
        volunteerTaskViewModel = new ViewModelProvider(requireActivity()).get(VolunteerTaskViewModel.class);
        if (geocodeExecutor == null || geocodeExecutor.isShutdown()) {
            geocodeExecutor = Executors.newSingleThreadExecutor();
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        initLocationTracking();
        restoreSessionFromViewModel();
        setupMap();
        setupMissionOverviewControls();
        setupTopOverviewBottomSheet();
        setupNavigationControls();
        setupDebugTools();
        applyRestoredUiState();
        bindMissionData(volunteerTaskViewModel.getPendingMission().getValue());
        ensureCurrentLocation();
    }

    @Override
    protected void observeViewModel() {
        volunteerMapViewModel.getRouteResult().observe(
                getViewLifecycleOwner(),
                resultObserver(this::renderRouteResult, this::showRouteError)
        );

        volunteerTaskViewModel.getPendingMission().observe(getViewLifecycleOwner(), this::bindMissionData);

        volunteerMapViewModel.getSimulatedPoint().observe(getViewLifecycleOwner(), point -> {
            if (point == null) {
                return;
            }
            applySimulatedPoint(point);
        });

        volunteerMapViewModel.getSimulationState().observe(getViewLifecycleOwner(), this::updateSimulationButtonState);
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        isRoutingLoading = isLoading;
        binding.progressRouting.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnCalculateRoute.setEnabled(!isLoading);
    }

    @Override
    public void onResume() {
        super.onResume();
        isMapScreenActive = true;
        shouldRefreshAutoStartOnNextFix = true;
        hasShownOutOfBoundsWarning = false;

        if (isMapResumeScheduled) {
            return;
        }
        
        // Early return check for binding
        if (binding == null) {
            return;
        }

        isMapResumeScheduled = true;

        // PHASE 1: Minimal map resume (fast return to main thread)
        if (mapView != null) {
            try {
                mapView.setUseDataConnection(true);
                mapView.onResume();
            } catch (Exception e) {
                // Ignore
            }
        }

        // PHASE 2: Lazy load overlays after UI is stable (500ms delay)
        mainHandler.postDelayed(() -> {
            if (mapView == null || !isMapScreenActive || binding == null) {
                isMapResumeScheduled = false;
                return;
            }

            try {
                // Restore cached instruction index from ViewModel
                int restoredInstructionIndex = volunteerMapViewModel.getCachedInstructionIndex();
                if (restoredInstructionIndex >= 0 && restoredInstructionIndex < currentInstructions.size()) {
                    cachedInstructionIndex = restoredInstructionIndex;
                } else {
                    cachedInstructionIndex = -1;  // Reset if invalid
                }

                // Load cached route polyline if available
                String cachedPolyline = volunteerMapViewModel.getLastPolyline();
                if (cachedPolyline != null && !cachedPolyline.trim().isEmpty()) {
                    try {
                        drawRoutePolyline(cachedPolyline, false);
                    } catch (Exception e) {
                        // Ignore polyline errors
                    }
                }

                // Load markers
                try {
                    updateMapMarkersIfNeeded();
                } catch (Exception e) {
                    // Ignore marker errors
                }

                // Update HUD with restored instruction index
                try {
                    updateHudInstructionsIfNeeded();
                } catch (Exception e) {
                    // Ignore HUD errors
                }

                // Restore cached dangerous zones
                List<RoutingRequestDto.DangerousZoneDto> cachedZones = 
                    volunteerMapViewModel.getCachedDangerousZones();
                if (!cachedZones.isEmpty()) {
                    try {
                        currentDangerousZones = cachedZones;
                        renderDangerousZones(cachedZones);
                    } catch (Exception e) {
                        // Ignore dangerous zone errors
                    }
                }
            } catch (Exception e) {
                // Catch any unexpected errors during lazy loading
            }

            isMapResumeScheduled = false;
        }, 500L);

        // Location updates with separate delay to avoid stalling
        mainHandler.postDelayed(this::startLocationUpdates, 300L);

        try {
            // Restore navigation mode from ViewModel
            isNavigationActive = volunteerMapViewModel.isNavigationActive();
            if (isNavigationActive) {
                enterNavigationMode();
            }

            // Resume simulation with restored point index
            volunteerMapViewModel.resumeSimulationIfNeeded();
            if (volunteerMapViewModel.isSimulationRunning()) {
                updateSimulationButtonState(Boolean.TRUE);
            }
        } catch (Exception e) {
            // Ignore restoration errors
        }
    }

    @Override
    public void onPause() {
        isMapScreenActive = false;
        isMapResumeScheduled = false;  // Clear resume flag

        // Save current state before pausing
        volunteerMapViewModel.setCachedInstructionIndex(cachedInstructionIndex);
        volunteerMapViewModel.setLastSimulationPointBeforePause(
            volunteerMapViewModel.getSimulationPointIndex()
        );
        volunteerMapViewModel.setCachedDangerousZones(currentDangerousZones);

        stopLocationUpdates();
        volunteerMapViewModel.pauseSimulationForBackground();
        if (mapView != null) {
            mapView.setUseDataConnection(false);
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        // CRITICAL: Clear all overlays immediately to free osmdroid resources
        if (mapView != null) {
            try {
                if (routePolyline != null) {
                    mapView.getOverlays().remove(routePolyline);
                    routePolyline = null;
                }
                if (routePolylineCasing != null) {
                    mapView.getOverlays().remove(routePolylineCasing);
                    routePolylineCasing = null;
                }
                if (startMarker != null) {
                    mapView.getOverlays().remove(startMarker);
                    startMarker = null;
                }
                if (endMarker != null) {
                    mapView.getOverlays().remove(endMarker);
                    endMarker = null;
                }
                if (currentMarker != null) {
                    mapView.getOverlays().remove(currentMarker);
                    currentMarker = null;
                }
                clearDangerousZoneOverlays();
                mapView.getOverlays().clear();
                
                // Disable data connection to save memory
                mapView.setUseDataConnection(false);
                mapView.onPause();
                mapView.onDetach();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }

        // Clear marker tracking
        lastStartMarkerPoint = null;
        lastEndMarkerPoint = null;
        lastCurrentMarkerPoint = null;
        lastMarkerUpdateAtMs = 0;
        routeProgressMeters.clear();
        cachedNearestRoutePointIndex = -1;
        topOverviewBottomSheetBehavior = null;

        stopLocationUpdates();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (geocodeExecutor != null) {
            geocodeExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    private void restoreSessionFromViewModel() {
        selectedStrategy = volunteerMapViewModel.getSelectedStrategy();
        avoidDangerousZones = volunteerMapViewModel.isAvoidDangerousZones();
        isNavigationActive = volunteerMapViewModel.isNavigationActive();
        isManualStartPoint = volunteerMapViewModel.isManualStartPoint();
        awaitDevStartPin = volunteerMapViewModel.isAwaitDevStartPin();
        isNetworkDropSimulated = volunteerMapViewModel.isNetworkDropSimulated();
        isTopOverviewExpanded = volunteerMapViewModel.isTopOverviewExpanded();

        startPoint = volunteerMapViewModel.getStartPoint();
        endPoint = volunteerMapViewModel.getEndPoint();
        currentPoint = volunteerMapViewModel.getCurrentPoint();

        routePoints.clear();
        routePoints.addAll(volunteerMapViewModel.getRoutePoints());
        rebuildRouteProgressCache();
        cachedNearestRoutePointIndex = -1;
        currentInstructions = volunteerMapViewModel.getInstructions();
    }

    private void applyRestoredUiState() {
        updateRouteOptionLabel();
        applyTopOverviewState();

        String cachedStartAddress = volunteerMapViewModel.getStartAddress();
        if (cachedStartAddress != null && !cachedStartAddress.trim().isEmpty()) {
            binding.tvStartAddress.setText(cachedStartAddress);
        }

        String cachedEndAddress = volunteerMapViewModel.getEndAddress();
        if (cachedEndAddress != null && !cachedEndAddress.trim().isEmpty()) {
            binding.tvEndAddress.setText(cachedEndAddress);
            binding.tvPrimaryDestination.setText(toCompactAddress(cachedEndAddress));
        } else {
            binding.tvPrimaryDestination.setText(R.string.volunteer_map_end_address_loading_short);
        }

        Double cachedDistance = volunteerMapViewModel.getLastDistanceMeters();
        if (cachedDistance != null) {
            binding.tvRouteDistance.setText(formatDistance(cachedDistance));
        }

        Long cachedDuration = volunteerMapViewModel.getLastDurationSeconds();
        if (cachedDuration != null) {
            binding.tvRouteDuration.setText(formatDuration(cachedDuration));
        }

        if (isNavigationActive) {
            enterNavigationMode();
        } else {
            exitNavigationMode();
        }
    }

    private void setupMap() {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView = binding.osmMapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        MapController controller = (MapController) mapView.getController();
        controller.setZoom(14.2);
        controller.setCenter(resolveInitialCenterPoint());

        if (BuildConfig.DEBUG) {
            GestureDetector gestureDetector = new GestureDetector(
                    requireContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public void onLongPress(@NonNull MotionEvent event) {
                            handleDebugLongPress(event);
                        }
                    }
            );

            mapView.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                // Handle tap for point selection
                if (event.getAction() == MotionEvent.ACTION_UP && pointSelectionMode != PointSelectionMode.NONE) {
                    handleMapTap(event);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ensureControlPanelAvailable();
                }
                return false;
            });
        } else {
            // Non-debug mode: add simple tap listener for point selection
            mapView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP && pointSelectionMode != PointSelectionMode.NONE) {
                    handleMapTap(event);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ensureControlPanelAvailable();
                }
                return false;
            });
        }

        isMapBaseInitialized = true;
        // NOTE: Polyline + markers loading is deferred to onResume() lazy loading phase
    }

    private void setupMissionOverviewControls() {
        binding.btnToggleTopPanel.setOnClickListener(v -> toggleTopOverviewPanel());
        binding.btnRouteOptions.setOnClickListener(v -> showRouteOptionBottomSheet());
        binding.btnCalculateRoute.setOnClickListener(v -> {
            hideTopOverviewPanelForRouteFocus();
            requestRouteCalculation();
        });
        binding.btnStartNavigation.setOnClickListener(v -> handleStartNavigationClick());
        binding.btnCallVictim.setOnClickListener(v -> openQuickDial());
        binding.fabOpenControlPanel.setOnClickListener(v -> openTopOverviewPanelFromFab());
        
        // Point selection buttons
        binding.btnSetStartPoint.setOnClickListener(v -> showStartPointModeMenu());
        binding.btnSetEndPoint.setOnClickListener(v -> setPointSelectionMode(PointSelectionMode.END));
        updatePointSelectionButtonStates();
        updateOpenControlFabVisibility();
    }

    private void showStartPointModeMenu() {
        if (binding == null) {
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_volunteer_start_mode, null, false);
        dialog.setContentView(view);

        View manualCard = view.findViewById(R.id.cardStartModeManual);
        View currentCard = view.findViewById(R.id.cardStartModeCurrent);

        manualCard.setOnClickListener(v -> {
            setPointSelectionMode(PointSelectionMode.START);
            showToast(getString(R.string.volunteer_map_pick_start_on_map_hint));
            dialog.dismiss();
        });

        currentCard.setOnClickListener(v -> {
            setStartPointToCurrentLocation();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setStartPointToCurrentLocation() {
        if (currentPoint == null) {
            showRouteError(getString(R.string.volunteer_map_missing_location));
            return;
        }

        if (!isWithinRoutingBounds(currentPoint)) {
            showRouteError(getString(R.string.volunteer_map_location_out_of_bounds));
            return;
        }

        startPoint = new GeoPoint(currentPoint.getLatitude(), currentPoint.getLongitude());
        volunteerMapViewModel.setStartPoint(startPoint);
        isManualStartPoint = true;
        shouldRefreshAutoStartOnNextFix = false;
        volunteerMapViewModel.setManualStartPoint(true);

        clearCurrentRoutePathState();
        reverseGeocodeAsync(startPoint, true);
        updateMapMarkers();
        showToast(getString(R.string.volunteer_map_start_set_to_current));
    }

    private void handleStartNavigationClick() {
        if (startPoint == null) {
            if (currentPoint == null || !isWithinRoutingBounds(currentPoint)) {
                showRouteError(getString(R.string.volunteer_map_missing_location));
                return;
            }

            startPoint = new GeoPoint(currentPoint.getLatitude(), currentPoint.getLongitude());
            volunteerMapViewModel.setStartPoint(startPoint);
            isManualStartPoint = true;
            volunteerMapViewModel.setManualStartPoint(true);
            shouldRefreshAutoStartOnNextFix = false;
            reverseGeocodeAsync(startPoint, true);
        }

        if (endPoint == null) {
            showRouteError(getString(R.string.volunteer_map_no_target_location));
            return;
        }

        if (!isWithinRoutingBounds(startPoint) || !isWithinRoutingBounds(endPoint)) {
            showRouteError(getString(R.string.volunteer_map_route_out_of_bounds));
            return;
        }

        pendingNavigationAfterRouteCalculation = true;
        requestRouteCalculation(true);
    }

    private void setupTopOverviewBottomSheet() {
        if (binding == null) {
            return;
        }

        try {
            topOverviewBottomSheetBehavior = BottomSheetBehavior.from(binding.cardTopOverview);
            defaultTopOverviewPeekHeightPx =
                    getResources().getDimensionPixelSize(R.dimen.volunteer_map_sheet_peek_height);
            minTopOverviewPeekHeightPx =
                getResources().getDimensionPixelSize(R.dimen.volunteer_map_sheet_collapsed_min_height);
            topOverviewBottomSheetBehavior.setHideable(false);
            topOverviewBottomSheetBehavior.setSkipCollapsed(false);
            topOverviewBottomSheetBehavior.setDraggable(true);
            topOverviewBottomSheetBehavior.setPeekHeight(defaultTopOverviewPeekHeightPx, false);
            topOverviewBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN && topOverviewBottomSheetBehavior != null) {
                        topOverviewBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        return;
                    }

                    if (newState != BottomSheetBehavior.STATE_EXPANDED
                            && newState != BottomSheetBehavior.STATE_COLLAPSED) {
                        return;
                    }
                    isTopOverviewExpanded = newState == BottomSheetBehavior.STATE_EXPANDED;
                    volunteerMapViewModel.setTopOverviewExpanded(isTopOverviewExpanded);
                    applyTopOverviewState();
                    updateOpenControlFabVisibility();
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    // No-op. State handling is enough for this screen.
                }
            });

            binding.cardTopOverview.post(() -> {
                if (topOverviewBottomSheetBehavior == null) {
                    return;
                }
                topOverviewBottomSheetBehavior.setState(
                        isTopOverviewExpanded
                                ? BottomSheetBehavior.STATE_EXPANDED
                                : BottomSheetBehavior.STATE_COLLAPSED
                );
                updateOpenControlFabVisibility();
            });
        } catch (Exception e) {
            topOverviewBottomSheetBehavior = null;
        }
    }

    private void openTopOverviewPanelFromFab() {
        if (binding == null) {
            return;
        }

        isTopOverviewAutoHiddenForRoute = false;
        if (topOverviewBottomSheetBehavior != null) {
            topOverviewBottomSheetBehavior.setPeekHeight(defaultTopOverviewPeekHeightPx, true);
        }
        binding.cardTopOverview.setVisibility(View.VISIBLE);
        isTopOverviewExpanded = true;
        volunteerMapViewModel.setTopOverviewExpanded(true);
        applyTopOverviewState();
        updateOpenControlFabVisibility();
    }

    private void updateOpenControlFabVisibility() {
        if (binding == null) {
            return;
        }

        binding.fabOpenControlPanel.setVisibility(View.VISIBLE);
        binding.fabOpenControlPanel.bringToFront();
    }

    private void ensureControlPanelAvailable() {
        if (binding == null || isNavigationActive || isTopOverviewAutoHiddenForRoute) {
            return;
        }

        if (binding.cardTopOverview.getVisibility() != View.VISIBLE) {
            binding.cardTopOverview.setVisibility(View.VISIBLE);
        }

        if (topOverviewBottomSheetBehavior != null
                && topOverviewBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            topOverviewBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        updateOpenControlFabVisibility();
    }

    private void hideTopOverviewPanelForRouteFocus() {
        if (binding == null || isNavigationActive) {
            return;
        }

        isTopOverviewAutoHiddenForRoute = true;
        isTopOverviewExpanded = false;
        volunteerMapViewModel.setTopOverviewExpanded(false);

        binding.layoutTopOverviewContent.setVisibility(View.GONE);
        binding.btnToggleTopPanel.setRotation(180f);
        binding.cardTopOverview.setVisibility(View.GONE);

        if (topOverviewBottomSheetBehavior != null
                && topOverviewBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
            topOverviewBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        updateOpenControlFabVisibility();
        binding.fabOpenControlPanel.bringToFront();
    }

    private void setPointSelectionMode(PointSelectionMode mode) {
        pointSelectionMode = mode;
        updatePointSelectionButtonStates();
    }

    private void updatePointSelectionButtonStates() {
        try {
            if (binding == null) return;
            
            boolean isSelectingStart = pointSelectionMode == PointSelectionMode.START;
            boolean isSelectingEnd = pointSelectionMode == PointSelectionMode.END;
            
            // Change colors to indicate selection state
            binding.btnSetStartPoint.setAlpha(isSelectingStart ? 1.0f : 0.7f);
            binding.btnSetEndPoint.setAlpha(isSelectingEnd ? 1.0f : 0.7f);
        } catch (Exception e) {
            // Ignore UI errors
        }
    }

    private void setupNavigationControls() {
        binding.btnCancelMission.setOnClickListener(v -> {
            exitNavigationMode();
            showToast(getString(R.string.volunteer_map_mission_cancelled));
        });

        binding.btnArrivedMission.setOnClickListener(v -> {
            showToast(getString(R.string.volunteer_map_arrived_success));
            exitNavigationMode();
        });
    }

    private void setupDebugTools() {
        boolean isDebug = BuildConfig.DEBUG;
        binding.btnDevSimulate.setVisibility(isDebug ? View.VISIBLE : View.GONE);
        binding.ivDevAvatar.setVisibility(isDebug ? View.VISIBLE : View.GONE);
        binding.tvDevHintSign.setVisibility(isDebug ? View.VISIBLE : View.GONE);

        if (!isDebug) {
            return;
        }

        binding.btnDevSimulate.setOnClickListener(v -> {
            if (volunteerMapViewModel.isSimulationRunning()) {
                volunteerMapViewModel.stopSimulation();
                return;
            }
            startSimulation();
        });

        binding.ivDevAvatar.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - lastAvatarTapAt > DEV_TAP_WINDOW_MS) {
                avatarTapCount = 0;
            }
            avatarTapCount++;
            lastAvatarTapAt = now;
            if (avatarTapCount >= 5) {
                avatarTapCount = 0;
                showDevPanelBottomSheet();
            }
        });
    }

    private void initLocationTracking() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateIntervalMillis(1500L)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isMapScreenActive) {
                    return;  // Skip location updates when fragment is hidden
                }
                
                Location location = locationResult.getLastLocation();
                if (location == null || volunteerMapViewModel.isSimulationRunning()) {
                    return;
                }
                applyCurrentLocation(new GeoPoint(location.getLatitude(), location.getLongitude()));
            }
        };
    }

    private void ensureCurrentLocation() {
        if (!hasLocationPermission()) {
            requestPermissions(
                    new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
            return;
        }

        fetchInitialLocationImmediately();
        startLocationUpdates();
    }

    private void fetchInitialLocationImmediately() {
        if (!hasLocationPermission()) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null || volunteerMapViewModel.isSimulationRunning()) {
                return;
            }
            applyCurrentLocation(new GeoPoint(location.getLatitude(), location.getLongitude()));
        });

        CancellationTokenSource tokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location == null || volunteerMapViewModel.isSimulationRunning()) {
                        return;
                    }
                    applyCurrentLocation(new GeoPoint(location.getLatitude(), location.getLongitude()));
                });
    }

    private void startLocationUpdates() {
        if (!hasLocationPermission() || locationCallback == null || fusedLocationClient == null) {
            return;
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (Exception e) {
            // Ignore location request errors
        }
    }

    private void stopLocationUpdates() {
        if (locationCallback != null && fusedLocationClient != null) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            } catch (Exception e) {
                // Ignore stop errors
            }
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        boolean granted = false;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                granted = true;
                break;
            }
        }

        if (!granted) {
            showRouteError(getString(R.string.volunteer_map_location_permission_denied));
            return;
        }

        fetchInitialLocationImmediately();
        startLocationUpdates();
    }

    private void applyCurrentLocation(@NonNull GeoPoint geoPoint) {
        currentPoint = geoPoint;
        volunteerMapViewModel.setCurrentPoint(geoPoint);
        centerOnCurrentLocationFirstTimeIfNeeded(geoPoint);

        if (!isManualStartPoint && !isNavigationActive && shouldRefreshAutoStartOnNextFix) {
            shouldRefreshAutoStartOnNextFix = false;
            if (!isWithinRoutingBounds(geoPoint)) {
                if (!hasShownOutOfBoundsWarning) {
                    hasShownOutOfBoundsWarning = true;
                    showRouteError(getString(R.string.volunteer_map_location_out_of_bounds));
                }
                return;
            }

            hasShownOutOfBoundsWarning = false;
            boolean startChanged = startPoint == null || distanceMeters(startPoint, geoPoint) > 20d;
            if (startChanged) {
                startPoint = geoPoint;
                volunteerMapViewModel.setStartPoint(geoPoint);
                clearCurrentRoutePathState();
                reverseGeocodeAsync(geoPoint, true);
                if (endPoint != null) {
                    requestRouteCalculation();
                }
            }
        }

        if (!isMapScreenActive) {
            return;
        }

        updateMapMarkersThrottled();
        updateHudInstructions();
    }

    private void centerOnCurrentLocationFirstTimeIfNeeded(@NonNull GeoPoint locationPoint) {
        if (hasCenteredOnInitialGps || mapView == null || !isWithinRoutingBounds(locationPoint)) {
            return;
        }

        try {
            mapView.getController().animateTo(locationPoint);
            hasCenteredOnInitialGps = true;
        } catch (Exception e) {
            // Ignore camera errors
        }
    }

    private void bindMissionData(@Nullable VolunteerMission mission) {
        if (mission != null) {
            String note = mission.getNote();
            if (note == null || note.trim().isEmpty()) {
                note = mission.getComment();
            }


            if (mission.getVictimLat() != null && mission.getVictimLng() != null && endPoint == null) {
                endPoint = new GeoPoint(mission.getVictimLat(), mission.getVictimLng());
                volunteerMapViewModel.setEndPoint(endPoint);
            }
        }

//        if (endPoint == null) {
//            endPoint = new GeoPoint(DEFAULT_END_LAT, DEFAULT_END_LON);
//            volunteerMapViewModel.setEndPoint(endPoint);
//        }

        reverseGeocodeAsync(endPoint, false);
        
        // Defer initial marker update to lazy loading phase
        if (isMapScreenActive) {
            try {
                updateMapMarkers();
            } catch (Exception e) {
                // Ignore - will be retried in onResume
            }
        }

//        if (startPoint != null && endPoint != null && routePoints.isEmpty()) {
//            requestRouteCalculation();
//        }
    }

    private void requestRouteCalculation() {
        requestRouteCalculation(false);
    }

    private void requestRouteCalculation(boolean bypassDebounce) {
        if (isRoutingLoading) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!bypassDebounce && now - lastRouteRequestAtMs < ROUTE_REQUEST_DEBOUNCE_MS) {
            return;
        }

        if (isNetworkDropSimulated) {
            showRouteError(getString(R.string.volunteer_map_network_dropped));
            return;
        }

        if (startPoint == null) {
            showRouteError(getString(R.string.volunteer_map_missing_location));
            return;
        }

        if (endPoint == null) {
            showRouteError(getString(R.string.volunteer_map_no_target_location));
            return;
        }

        if (!isWithinRoutingBounds(startPoint) || !isWithinRoutingBounds(endPoint)) {
            showRouteError(getString(R.string.volunteer_map_route_out_of_bounds));
            return;
        }

        RoutingRequestDto requestDto = new RoutingRequestDto(
                startPoint.getLatitude(),
                startPoint.getLongitude(),
                endPoint.getLatitude(),
                endPoint.getLongitude(),
                selectedStrategy,
                avoidDangerousZones ? buildHardcodedDangerousZones() : Collections.emptyList()
        );

        lastRouteRequestAtMs = now;
        renderDangerousZones(requestDto.getDangerousZones());
        volunteerMapViewModel.calculateRoute(requestDto);
    }

    private void renderRouteResult(@Nullable RoutingResponseDto response) {
        if (response == null) {
            showRouteError(getString(R.string.volunteer_map_route_failed));
            return;
        }

        currentInstructions = response.getInstructions() != null
                ? response.getInstructions()
                : Collections.emptyList();
        volunteerMapViewModel.setInstructions(currentInstructions);

        // Reset instruction index cache when new route is loaded
        cachedInstructionIndex = -1;
        cachedNearestRoutePointIndex = -1;
        lastInstructionIndexLocation = null;

        double distance = response.getDistance() != null ? response.getDistance() : 0d;
        long duration = response.getDuration() != null ? response.getDuration() : 0L;
        volunteerMapViewModel.setLastDistanceMeters(distance);
        volunteerMapViewModel.setLastDurationSeconds(duration);
        binding.tvRouteDistance.setText(formatDistance(distance));
        binding.tvRouteDuration.setText(formatDuration(duration));

        drawRoutePolyline(response.getPolyline(), true);
        updateMapMarkers();
        updateHudInstructions();

        if (pendingNavigationAfterRouteCalculation) {
            pendingNavigationAfterRouteCalculation = false;
            enterNavigationMode();
            return;
        }

        if (!isNavigationActive) {
            showToast(getString(R.string.volunteer_map_route_success));
        }
    }

    private void drawRoutePolyline(@Nullable String encodedPolyline, boolean cachePolyline) {
        routePoints.clear();

        if (routePolylineCasing != null) {
            mapView.getOverlays().remove(routePolylineCasing);
            routePolylineCasing = null;
        }

        if (routePolyline != null) {
            mapView.getOverlays().remove(routePolyline);
            routePolyline = null;
        }

        if (encodedPolyline == null || encodedPolyline.trim().isEmpty()) {
            showRouteError(getString(R.string.volunteer_map_route_missing_polyline));
            mapView.invalidate();
            if (cachePolyline) {
                volunteerMapViewModel.setLastPolyline(null);
                volunteerMapViewModel.setRoutePoints(Collections.emptyList());
            }
            return;
        }

        List<GeoPoint> decodedPoints = decodePolyline(encodedPolyline);
        if (decodedPoints.isEmpty()) {
            showRouteError(getString(R.string.volunteer_map_route_missing_polyline));
            mapView.invalidate();
            return;
        }

        List<GeoPoint> smoothedPoints = smoothRoutePoints(decodedPoints);
        routePoints.addAll(smoothedPoints);
        rebuildRouteProgressCache();
        cachedNearestRoutePointIndex = -1;

        routePolylineCasing = new Polyline();
        routePolylineCasing.setPoints(smoothedPoints);
        routePolylineCasing.setColor(ContextCompat.getColor(requireContext(), R.color.volunteer_map_route_casing));
        routePolylineCasing.setWidth(getResources().getDimension(R.dimen.volunteer_map_route_casing_stroke_width));
        routePolylineCasing.getOutlinePaint().setAntiAlias(true);
        routePolylineCasing.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        routePolylineCasing.getOutlinePaint().setStrokeJoin(Paint.Join.ROUND);
        mapView.getOverlays().add(routePolylineCasing);

        routePolyline = new Polyline();
        routePolyline.setPoints(smoothedPoints);
        routePolyline.setColor(ContextCompat.getColor(requireContext(), R.color.volunteer_map_route_core));
        routePolyline.setWidth(getResources().getDimension(R.dimen.volunteer_map_route_stroke_width));
        routePolyline.getOutlinePaint().setAntiAlias(true);
        routePolyline.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        routePolyline.getOutlinePaint().setStrokeJoin(Paint.Join.ROUND);
        mapView.getOverlays().add(routePolyline);

        if (cachePolyline) {
            volunteerMapViewModel.setLastPolyline(encodedPolyline);
            volunteerMapViewModel.setRoutePoints(smoothedPoints);
        }

        BoundingBox bounds = BoundingBox.fromGeoPointsSafe(smoothedPoints);
        mapView.zoomToBoundingBox(bounds, true, getResources().getDimensionPixelSize(R.dimen.spacing_xl));
        mapView.invalidate();
    }

    @NonNull
    private List<GeoPoint> smoothRoutePoints(@NonNull List<GeoPoint> originalPoints) {
        if (originalPoints.size() < 2) {
            return originalPoints;
        }

        List<GeoPoint> smoothed = new ArrayList<>();
        smoothed.add(originalPoints.get(0));

        for (int i = 1; i < originalPoints.size(); i++) {
            GeoPoint from = originalPoints.get(i - 1);
            GeoPoint to = originalPoints.get(i);

            double segmentDistance = distanceMeters(from, to);
            int steps = (int) Math.ceil(segmentDistance / ROUTE_SMOOTHING_SEGMENT_METERS);
            steps = Math.max(1, Math.min(steps, ROUTE_SMOOTHING_MAX_STEPS));

            for (int step = 1; step < steps; step++) {
                double fraction = step / (double) steps;
                double lat = from.getLatitude() + ((to.getLatitude() - from.getLatitude()) * fraction);
                double lon = from.getLongitude() + ((to.getLongitude() - from.getLongitude()) * fraction);
                smoothed.add(new GeoPoint(lat, lon));
            }

            smoothed.add(to);
        }

        return smoothed;
    }

    private void updateMapMarkers() {
        try {
            // Smart marker reuse: only rebuild if position changed significantly
            if (startPoint != null) {
                if (startMarker == null || 
                    lastStartMarkerPoint == null || 
                    distanceMeters(startPoint, lastStartMarkerPoint) > 5d) {
                    
                    if (startMarker != null) {
                        mapView.getOverlays().remove(startMarker);
                    }
                    
                    startMarker = new Marker(mapView);
                    startMarker.setPosition(startPoint);
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    startMarker.setTitle(getString(R.string.volunteer_map_start_label));
                    startMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.bg_map_start_marker));
                    configureMarkerPopup(startMarker, getString(R.string.volunteer_map_start_label));
                    mapView.getOverlays().add(startMarker);
                    lastStartMarkerPoint = startPoint;
                }
            } else if (startMarker != null) {
                mapView.getOverlays().remove(startMarker);
                startMarker = null;
                lastStartMarkerPoint = null;
            }

            if (endPoint != null) {
                if (endMarker == null || 
                    lastEndMarkerPoint == null || 
                    distanceMeters(endPoint, lastEndMarkerPoint) > 5d) {
                    
                    if (endMarker != null) {
                        mapView.getOverlays().remove(endMarker);
                    }
                    
                    endMarker = new Marker(mapView);
                    endMarker.setPosition(endPoint);
                    endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    endMarker.setTitle(getString(R.string.volunteer_map_end_label));
                    endMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.bg_map_end_marker));
                    configureMarkerPopup(endMarker, getString(R.string.volunteer_map_end_label));
                    mapView.getOverlays().add(endMarker);
                    lastEndMarkerPoint = endPoint;
                }
            } else if (endMarker != null) {
                mapView.getOverlays().remove(endMarker);
                endMarker = null;
                lastEndMarkerPoint = null;
            }

            GeoPoint dynamicPoint = currentPoint != null ? currentPoint : startPoint;
            if (dynamicPoint != null) {
                if (currentMarker == null || 
                    lastCurrentMarkerPoint == null || 
                    distanceMeters(dynamicPoint, lastCurrentMarkerPoint) > 3d) {
                    
                    if (currentMarker != null) {
                        mapView.getOverlays().remove(currentMarker);
                    }
                    
                    currentMarker = new Marker(mapView);
                    currentMarker.setPosition(dynamicPoint);
                    currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    currentMarker.setTitle(getString(R.string.volunteer_map_volunteer_address_label));
                    currentMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.bg_map_current_marker));
                    configureMarkerPopup(currentMarker, getString(R.string.volunteer_map_volunteer_address_label));
                    mapView.getOverlays().add(currentMarker);
                    lastCurrentMarkerPoint = dynamicPoint;
                }
            } else if (currentMarker != null) {
                mapView.getOverlays().remove(currentMarker);
                currentMarker = null;
                lastCurrentMarkerPoint = null;
            }

            showMarkerPopups();
            mapView.invalidate();
        } catch (Exception e) {
            // Ignore marker update failures
        }
    }

    private void configureMarkerPopup(@NonNull Marker marker, @NonNull String title) {
        marker.setTitle(title);
        marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);
        marker.setOnMarkerClickListener((clickedMarker, clickedMapView) -> {
            clickedMarker.showInfoWindow();
            return true;
        });
    }

    private void showMarkerPopups() {
        if (startMarker != null) {
            startMarker.showInfoWindow();
        }
        if (endMarker != null) {
            endMarker.showInfoWindow();
        }
        if (currentMarker != null) {
            currentMarker.showInfoWindow();
        }
    }

    private void updateMapMarkersIfNeeded() {
        // Only rebuild markers if they're not already visible
        if (mapView == null) {
            return;  // Map not initialized yet
        }
        
        if (startMarker == null || endMarker == null || currentMarker == null) {
            updateMapMarkersThrottled();
        }
    }

    private void updateMapMarkersThrottled() {
        long now = System.currentTimeMillis();
        if (now - lastMarkerUpdateAtMs < MARKER_UPDATE_THROTTLE_MS) {
            return;  // Skip if called too frequently
        }
        lastMarkerUpdateAtMs = now;
        updateMapMarkers();
    }

    private void updateHudInstructionsIfNeeded() {
        if (binding == null || !isNavigationActive || currentInstructions.isEmpty()) {
            return;
        }

        try {
            int newIndex = resolveInstructionIndex();
            // Only update if instruction index changed
            if (newIndex != cachedInstructionIndex) {
                cachedInstructionIndex = newIndex;
                updateHudInstructions();
            }
        } catch (Exception e) {
            // Ignore update errors
        }
    }

    private void renderDangerousZones(@NonNull List<RoutingRequestDto.DangerousZoneDto> zones) {
        // Save zones for restoration
        currentDangerousZones = new ArrayList<>(zones);
        volunteerMapViewModel.setCachedDangerousZones(zones);

        clearDangerousZoneOverlays();

        for (RoutingRequestDto.DangerousZoneDto zone : zones) {
            if (zone.getGeometry() == null || zone.getGeometry().getCoordinates().isEmpty()) {
                continue;
            }
            List<List<Double>> ring = zone.getGeometry().getCoordinates().get(0);
            if (ring == null || ring.size() < 3) {
                continue;
            }

            List<GeoPoint> polygonPoints = new ArrayList<>();
            for (List<Double> coordinate : ring) {
                if (coordinate == null || coordinate.size() < 2) {
                    continue;
                }
                polygonPoints.add(new GeoPoint(coordinate.get(1), coordinate.get(0)));
            }

            if (polygonPoints.size() < 3) {
                continue;
            }

            Polygon polygon = new Polygon();
            polygon.setPoints(polygonPoints);
            polygon.setFillColor(ContextCompat.getColor(requireContext(), R.color.volunteer_map_polygon_fill));
            polygon.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.volunteer_map_polygon_stroke));
            polygon.setStrokeWidth(getResources().getDimension(R.dimen.volunteer_map_polygon_stroke_width));
            mapView.getOverlays().add(polygon);
            dangerousZoneOverlays.add(polygon);
        }

        mapView.invalidate();
    }

    private void clearDangerousZoneOverlays() {
        for (Polygon polygon : dangerousZoneOverlays) {
            mapView.getOverlays().remove(polygon);
        }
        dangerousZoneOverlays.clear();
    }

    private void enterNavigationMode() {
        isNavigationActive = true;
        volunteerMapViewModel.setNavigationActive(true);
        
        if (binding == null) {
            return;
        }
        
        try {
            binding.cardTopOverview.setVisibility(View.GONE);
            binding.cardNavigationHud.setVisibility(View.VISIBLE);
            binding.layoutNavigationActions.setVisibility(View.VISIBLE);
            updateHudInstructions();
            updateOpenControlFabVisibility();
        } catch (Exception e) {
            // Ignore UI errors
        }
    }

    private void exitNavigationMode() {
        isNavigationActive = false;
        volunteerMapViewModel.setNavigationActive(false);
        
        if (binding == null) {
            return;
        }
        
        try {
            binding.cardTopOverview.setVisibility(
                    isTopOverviewAutoHiddenForRoute ? View.GONE : View.VISIBLE
            );
            binding.cardNavigationHud.setVisibility(View.GONE);
            binding.layoutNavigationActions.setVisibility(View.GONE);
            if (!isTopOverviewAutoHiddenForRoute) {
                applyTopOverviewState();
            }
            updateOpenControlFabVisibility();
        } catch (Exception e) {
            // Ignore UI errors
        }
    }

    private void toggleTopOverviewPanel() {
        isTopOverviewExpanded = !isTopOverviewExpanded;
        volunteerMapViewModel.setTopOverviewExpanded(isTopOverviewExpanded);
        applyTopOverviewState();
    }

    private void applyTopOverviewState() {
        if (binding == null) {
            return;
        }
        
        try {
            if (isTopOverviewAutoHiddenForRoute) {
                binding.cardTopOverview.setVisibility(View.GONE);
                updateOpenControlFabVisibility();
                return;
            }

            binding.cardTopOverview.setVisibility(View.VISIBLE);
            binding.layoutTopOverviewContent.setVisibility(isTopOverviewExpanded ? View.VISIBLE : View.GONE);
            binding.btnToggleTopPanel.setRotation(isTopOverviewExpanded ? 0f : 180f);

            if (topOverviewBottomSheetBehavior != null) {
                if (isTopOverviewExpanded) {
                    topOverviewBottomSheetBehavior.setPeekHeight(defaultTopOverviewPeekHeightPx, true);
                    if (topOverviewBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                        topOverviewBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                } else {
                    binding.cardTopOverview.post(() -> {
                        if (binding == null || topOverviewBottomSheetBehavior == null) {
                            return;
                        }

                        int measuredHeight = binding.cardTopOverview.getHeight();
                        int collapsedHeight = Math.max(measuredHeight, minTopOverviewPeekHeightPx);
                        if (collapsedHeight > 0) {
                            topOverviewBottomSheetBehavior.setPeekHeight(collapsedHeight, true);
                        }

                        if (topOverviewBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
                            topOverviewBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        }
                    });
                }
            }
            updateOpenControlFabVisibility();
        } catch (Exception e) {
            // Ignore UI errors
        }
    }

    private void updateHudInstructions() {
        if (binding == null || !isNavigationActive) {
            return;
        }

        try {
            if (currentInstructions.isEmpty()) {
                binding.tvHudPrimaryInstruction.setText(R.string.volunteer_map_instruction_continue);
                binding.tvHudSecondaryInstruction.setText(R.string.volunteer_map_hud_waiting);
                return;
            }

            int currentIndex = resolveInstructionIndex();
            
            // Validate index is within bounds
            if (currentIndex < 0 || currentIndex >= currentInstructions.size()) {
                binding.tvHudPrimaryInstruction.setText(R.string.volunteer_map_instruction_continue);
                binding.tvHudSecondaryInstruction.setText(R.string.volunteer_map_hud_waiting);
                return;
            }

            RoutingResponseDto.InstructionDto primary = currentInstructions.get(currentIndex);
            cachedInstructionIndex = currentIndex;
            double remainingDistance = resolveRemainingDistanceForInstruction(currentIndex);
            binding.tvHudPrimaryInstruction.setText(buildInstructionLabel(primary, remainingDistance));

            int nextIndex = findNextDistinctInstructionIndex(currentIndex);
            if (nextIndex >= 0 && nextIndex < currentInstructions.size()) {
                RoutingResponseDto.InstructionDto next = currentInstructions.get(nextIndex);
                binding.tvHudSecondaryInstruction.setText(buildInstructionLabel(next));
                return;
            }

            binding.tvHudSecondaryInstruction.setText(R.string.volunteer_map_hud_waiting);
        } catch (Exception e) {
            // Silently ignore HUD update errors
            if (binding != null) {
                binding.tvHudPrimaryInstruction.setText(R.string.volunteer_map_instruction_continue);
                binding.tvHudSecondaryInstruction.setText(R.string.volunteer_map_hud_waiting);
            }
        }
    }

    private int resolveInstructionIndex() {
        if (currentInstructions.isEmpty() || routePoints.isEmpty()) {
            return 0;
        }

        GeoPoint reference = currentPoint != null ? currentPoint : startPoint;
        if (reference == null) {
            return 0;
        }

        // Skip recalculation when movement is minimal to avoid noisy HUD switching.
        if (lastInstructionIndexLocation != null
                && distanceMeters(reference, lastInstructionIndexLocation)
                < INSTRUCTION_UPDATE_LOCATION_THRESHOLD_METERS
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
        return Math.max(mappedIndex, 0);
    }

    private int findNextDistinctInstructionIndex(int currentIndex) {
        if (currentIndex < 0 || currentIndex >= currentInstructions.size()) {
            return -1;
        }

        String currentLabel = buildCommandWithRoad(currentInstructions.get(currentIndex)).trim();
        for (int i = currentIndex + 1; i < currentInstructions.size(); i++) {
            String nextLabel = buildCommandWithRoad(currentInstructions.get(i)).trim();
            if (!nextLabel.equalsIgnoreCase(currentLabel)) {
                return i;
            }
        }
        return -1;
    }

    private int findNearestRoutePointIndex(@NonNull GeoPoint reference) {
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

    private double resolveRemainingDistanceForInstruction(int instructionIndex) {
        if (instructionIndex < 0 || instructionIndex >= currentInstructions.size()) {
            return 0d;
        }

        RoutingResponseDto.InstructionDto instruction = currentInstructions.get(instructionIndex);
        double instructionDistance =
                instruction != null && instruction.getDistance() != null
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
            return instructionDistance;
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

    private void rebuildRouteProgressCache() {
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

    private String buildInstructionLabel(@Nullable RoutingResponseDto.InstructionDto instruction) {
        return buildInstructionLabel(instruction, -1d);
    }

    private String buildInstructionLabel(
            @Nullable RoutingResponseDto.InstructionDto instruction,
            double overrideDistanceMeters
    ) {
        if (instruction == null) {
            return getString(R.string.volunteer_map_instruction_continue);
        }

        String command = buildCommandWithRoad(instruction);

        double distance = overrideDistanceMeters >= 0d
                ? overrideDistanceMeters
                : instruction.getDistance() != null ? instruction.getDistance() : 0d;
        return command + " - " + formatDistance(distance);
    }

    private String buildCommandWithRoad(@Nullable RoutingResponseDto.InstructionDto instruction) {
        if (instruction == null) {
            return getString(R.string.volunteer_map_instruction_continue);
        }

        String command = instruction.getCommand();
        if (command == null || command.trim().isEmpty()) {
            command = getString(R.string.volunteer_map_instruction_continue);
        } else {
            command = command.trim();
        }

        String road = instruction.getName();
        if (road == null || road.trim().isEmpty()) {
            return command;
        }

        return getString(R.string.volunteer_map_instruction_with_road, command, road.trim());
    }

    private void openQuickDial() {
        VolunteerMission mission = volunteerTaskViewModel.getPendingMission().getValue();
        String candidatePhone = mission != null ? extractPhoneNumber(mission.getComment()) : null;
        if (candidatePhone == null || candidatePhone.trim().isEmpty()) {
            candidatePhone = getString(R.string.volunteer_map_call_fallback_number);
        }

        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + candidatePhone));
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @Nullable
    private String extractPhoneNumber(@Nullable String source) {
        if (source == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("(\\+?\\d[\\d\\s.-]{8,}\\d)");
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("[^\\d+]", "");
        }
        return null;
    }

    private void showRouteOptionBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setCanceledOnTouchOutside(false);
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_volunteer_route_options, null, false);
        dialog.setContentView(view);

        RadioGroup radioGroup = view.findViewById(R.id.rgStrategy);
        SwitchMaterial avoidSwitch = view.findViewById(R.id.switchAvoidDangerous);
        MaterialButton applyButton = view.findViewById(R.id.btnApplyOptions);

        radioGroup.check(resolveRadioByStrategy(selectedStrategy));
        avoidSwitch.setChecked(avoidDangerousZones);

        applyButton.setOnClickListener(v -> {
            selectedStrategy = resolveStrategyByRadio(radioGroup.getCheckedRadioButtonId());
            avoidDangerousZones = avoidSwitch.isChecked();
            volunteerMapViewModel.setSelectedStrategy(selectedStrategy);
            volunteerMapViewModel.setAvoidDangerousZones(avoidDangerousZones);
            updateRouteOptionLabel();
            dialog.dismiss();
            requestRouteCalculation();
        });

        dialog.show();
    }

    private int resolveRadioByStrategy(@NonNull String strategy) {
        switch (strategy) {
            case STRATEGY_URGENT:
                return R.id.rbStrategyUrgent;
            case STRATEGY_SAFE:
                return R.id.rbStrategySafe;
            case STRATEGY_HEAVY_AID:
                return R.id.rbStrategyHeavyAid;
            case STRATEGY_COMMUNITY:
                return R.id.rbStrategyCommunity;
            default:
                return R.id.rbStrategyOffroad;
        }
    }

    private String resolveStrategyByRadio(int checkedId) {
        if (checkedId == R.id.rbStrategyUrgent) {
            return STRATEGY_URGENT;
        }
        if (checkedId == R.id.rbStrategySafe) {
            return STRATEGY_SAFE;
        }
        if (checkedId == R.id.rbStrategyHeavyAid) {
            return STRATEGY_HEAVY_AID;
        }
        if (checkedId == R.id.rbStrategyCommunity) {
            return STRATEGY_COMMUNITY;
        }
        return STRATEGY_OFFROAD;
    }

    private void updateRouteOptionLabel() {
        String strategyLabel;
        switch (selectedStrategy) {
            case STRATEGY_URGENT:
                strategyLabel = getString(R.string.volunteer_map_strategy_urgent);
                break;
            case STRATEGY_SAFE:
                strategyLabel = getString(R.string.volunteer_map_strategy_safe);
                break;
            case STRATEGY_HEAVY_AID:
                strategyLabel = getString(R.string.volunteer_map_strategy_heavy_aid);
                break;
            case STRATEGY_COMMUNITY:
                strategyLabel = getString(R.string.volunteer_map_strategy_community);
                break;
            default:
                strategyLabel = getString(R.string.volunteer_map_strategy_offroad);
                break;
        }

        String suffix = avoidDangerousZones ? " - " + getString(R.string.volunteer_map_avoid_dangerous) : "";
        binding.btnRouteOptions.setText(getString(R.string.volunteer_map_route_options) + ": " + strategyLabel + suffix);
    }

    private void showDevPanelBottomSheet() {
        if (!BuildConfig.DEBUG) {
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setCanceledOnTouchOutside(false);
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_volunteer_dev_panel, null, false);
        dialog.setContentView(view);

        TextInputEditText etCoordinates = view.findViewById(R.id.etDevCoordinates);
        MaterialButton btnApplyCoordinates = view.findViewById(R.id.btnDevApplyCoordinates);
        MaterialButton btnScenario2Km = view.findViewById(R.id.btnDevScenario2km);
        MaterialButton btnScenarioNetwork = view.findViewById(R.id.btnDevScenarioNetworkDrop);
        MaterialButton btnClear = view.findViewById(R.id.btnDevClearMarkers);

        btnScenarioNetwork.setText(isNetworkDropSimulated
                ? R.string.volunteer_map_dev_scenario_network_restore
                : R.string.volunteer_map_dev_scenario_network_drop);

        btnApplyCoordinates.setOnClickListener(v -> {
            String input = etCoordinates.getText() != null ? etCoordinates.getText().toString().trim() : "";
            GeoPoint parsed = parseCoordinateInput(input);
            if (parsed == null) {
                showRouteError(getString(R.string.volunteer_map_dev_invalid_coordinates));
                return;
            }

            endPoint = parsed;
            volunteerMapViewModel.setEndPoint(parsed);
            clearCurrentRoutePathState();
            reverseGeocodeAsync(endPoint, false);
            updateMapMarkers();
            dialog.dismiss();
        });

        btnScenario2Km.setOnClickListener(v -> {
            if (startPoint == null) {
                showRouteError(getString(R.string.volunteer_map_missing_location));
                return;
            }

            endPoint = new GeoPoint(startPoint.getLatitude() + 0.018, startPoint.getLongitude() + 0.006);
            volunteerMapViewModel.setEndPoint(endPoint);
            clearCurrentRoutePathState();
            reverseGeocodeAsync(endPoint, false);
            updateMapMarkers();
            dialog.dismiss();
        });

        btnScenarioNetwork.setOnClickListener(v -> {
            isNetworkDropSimulated = !isNetworkDropSimulated;
            volunteerMapViewModel.setNetworkDropSimulated(isNetworkDropSimulated);
            showToast(isNetworkDropSimulated
                    ? getString(R.string.volunteer_map_network_dropped)
                    : getString(R.string.volunteer_map_route_success));
            dialog.dismiss();
        });

        btnClear.setOnClickListener(v -> {
            clearMapState();
            dialog.dismiss();
        });

        dialog.show();
    }

    @Nullable
    private GeoPoint parseCoordinateInput(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        String[] parts = input.split(",");
        if (parts.length != 2) {
            return null;
        }

        try {
            return new GeoPoint(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void clearMapState() {
        if (routePolylineCasing != null) {
            mapView.getOverlays().remove(routePolylineCasing);
            routePolylineCasing = null;
        }

        if (routePolyline != null) {
            mapView.getOverlays().remove(routePolyline);
            routePolyline = null;
        }

        clearDangerousZoneOverlays();
        routePoints.clear();
        routeProgressMeters.clear();
        currentInstructions = Collections.emptyList();
        currentDangerousZones = Collections.emptyList();
        cachedInstructionIndex = -1;
        cachedNearestRoutePointIndex = -1;
        lastInstructionIndexLocation = null;

        volunteerMapViewModel.stopSimulation();
        volunteerMapViewModel.clearRouteData();
        volunteerMapViewModel.setInstructions(Collections.emptyList());
        volunteerMapViewModel.setRoutePoints(Collections.emptyList());
        volunteerMapViewModel.setCachedDangerousZones(Collections.emptyList());

        endPoint = null;
        volunteerMapViewModel.setEndPoint(null);
        updateMapMarkers();

        binding.tvRouteDistance.setText(R.string.volunteer_map_distance_default);
        binding.tvRouteDuration.setText(R.string.volunteer_map_duration_default);
        binding.tvHudPrimaryInstruction.setText(R.string.volunteer_map_instruction_continue);
        binding.tvHudSecondaryInstruction.setText(R.string.volunteer_map_hud_waiting);
    }

    private void clearCurrentRoutePathState() {
        if (mapView == null) {
            return;
        }

        if (routePolylineCasing != null) {
            mapView.getOverlays().remove(routePolylineCasing);
            routePolylineCasing = null;
        }

        if (routePolyline != null) {
            mapView.getOverlays().remove(routePolyline);
            routePolyline = null;
        }

        clearDangerousZoneOverlays();
        routePoints.clear();
        routeProgressMeters.clear();
        currentInstructions = Collections.emptyList();
        currentDangerousZones = Collections.emptyList();
        cachedInstructionIndex = -1;
        cachedNearestRoutePointIndex = -1;
        lastInstructionIndexLocation = null;
        pendingNavigationAfterRouteCalculation = false;

        volunteerMapViewModel.setLastPolyline(null);
        volunteerMapViewModel.setRoutePoints(Collections.emptyList());
        volunteerMapViewModel.setInstructions(Collections.emptyList());
        volunteerMapViewModel.setCachedDangerousZones(Collections.emptyList());
        volunteerMapViewModel.setLastDistanceMeters(null);
        volunteerMapViewModel.setLastDurationSeconds(null);

        if (binding != null) {
            binding.tvRouteDistance.setText(R.string.volunteer_map_distance_default);
            binding.tvRouteDuration.setText(R.string.volunteer_map_duration_default);
            if (!isNavigationActive) {
                binding.tvHudPrimaryInstruction.setText(R.string.volunteer_map_instruction_continue);
                binding.tvHudSecondaryInstruction.setText(R.string.volunteer_map_hud_waiting);
            }
        }

        mapView.invalidate();
    }

    private void handleDebugLongPress(@NonNull MotionEvent event) {
        GeoPoint point = (GeoPoint) mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());
        if (point == null) {
            return;
        }

        if (awaitDevStartPin) {
            startPoint = point;
            currentPoint = point;
            isManualStartPoint = true;
            volunteerMapViewModel.setManualStartPoint(true);
            volunteerMapViewModel.setStartPoint(point);
            volunteerMapViewModel.setCurrentPoint(point);
            clearCurrentRoutePathState();
            reverseGeocodeAsync(startPoint, true);
            showToast(getString(R.string.volunteer_map_dev_pin_start));
        } else {
            endPoint = point;
            volunteerMapViewModel.setEndPoint(point);
            clearCurrentRoutePathState();
            reverseGeocodeAsync(endPoint, false);
            showToast(getString(R.string.volunteer_map_dev_pin_end));
        }

        awaitDevStartPin = !awaitDevStartPin;
        volunteerMapViewModel.setAwaitDevStartPin(awaitDevStartPin);
        updateMapMarkers();
    }

    private void handleMapTap(@NonNull MotionEvent event) {
        if (mapView == null || pointSelectionMode == PointSelectionMode.NONE) {
            return;
        }

        try {
            // Convert screen coordinates to map coordinates
            GeoPoint tapPoint = (GeoPoint) mapView.getProjection()
                    .fromPixels((int) event.getX(), (int) event.getY());

            if (tapPoint == null) {
                return;
            }

            // Validate tap point is within routing bounds
            if (!isWithinRoutingBounds(tapPoint)) {
                showToast(getString(R.string.volunteer_map_route_out_of_bounds));
                return;
            }

            if (pointSelectionMode == PointSelectionMode.START) {
                startPoint = tapPoint;
                volunteerMapViewModel.setStartPoint(tapPoint);
                isManualStartPoint = true;
                volunteerMapViewModel.setManualStartPoint(true);
                clearCurrentRoutePathState();
                reverseGeocodeAsync(tapPoint, true);
                showToast(getString(R.string.volunteer_map_point_start_set));
            } else if (pointSelectionMode == PointSelectionMode.END) {
                endPoint = tapPoint;
                volunteerMapViewModel.setEndPoint(tapPoint);
                clearCurrentRoutePathState();
                reverseGeocodeAsync(tapPoint, false);
                showToast(getString(R.string.volunteer_map_point_end_set));
            }

            updateMapMarkers();
            pointSelectionMode = PointSelectionMode.NONE;  // Exit selection mode
            updatePointSelectionButtonStates();
        } catch (Exception e) {
            // Ignore tap errors
        }
    }

    private void startSimulation() {
        if (!BuildConfig.DEBUG || routePoints.isEmpty()) {
            showRouteError(getString(R.string.volunteer_map_no_route_for_simulation));
            return;
        }

        if (!isNavigationActive) {
            enterNavigationMode();
        }
        volunteerMapViewModel.startSimulationFromCurrentRoute();
    }

    private void applySimulatedPoint(@NonNull GeoPoint point) {
        currentPoint = point;
        volunteerMapViewModel.setCurrentPoint(point);

        if (!isMapScreenActive) {
            return;
        }

        updateMapMarkersThrottled();
        updateHudInstructions();
    }

    private void updateSimulationButtonState(@Nullable Boolean isRunning) {
        boolean running = isRunning != null && isRunning;
        if (!BuildConfig.DEBUG || binding == null) {
            return;
        }
        binding.btnDevSimulate.setText(running
                ? R.string.volunteer_map_dev_simulate_stop
                : R.string.volunteer_map_dev_simulate);
    }

    private void reverseGeocodeAsync(@NonNull GeoPoint point, boolean forStart) {
        if (geocodeExecutor == null || geocodeExecutor.isShutdown()) {
            return;
        }
        geocodeExecutor.execute(() -> {
            String resolved = requestAddressFromNominatim(point);
            if (resolved == null || resolved.trim().isEmpty()) {
                return;
            }

            String addressText = getString(
                    R.string.volunteer_map_address_format,
                    getString(forStart
                            ? R.string.volunteer_map_volunteer_address_label
                            : R.string.volunteer_map_victim_address_label),
                    resolved
            );

            if (forStart) {
                volunteerMapViewModel.setStartAddress(addressText);
            } else {
                volunteerMapViewModel.setEndAddress(addressText);
            }

            if (binding == null) {
                return;
            }

            mainHandler.post(() -> {
                if (binding == null) {
                    return;
                }
                if (forStart) {
                    binding.tvStartAddress.setText(addressText);
                } else {
                    binding.tvEndAddress.setText(addressText);
                    binding.tvPrimaryDestination.setText(toCompactAddress(addressText));
                }
            });
        });
    }

    @NonNull
    private String toCompactAddress(@Nullable String addressText) {
        if (addressText == null || addressText.trim().isEmpty()) {
            return getString(R.string.volunteer_map_end_address_loading_short);
        }

        int separatorIndex = addressText.indexOf(':');
        if (separatorIndex >= 0 && separatorIndex < addressText.length() - 1) {
            return addressText.substring(separatorIndex + 1).trim();
        }
        return addressText.trim();
    }

    @Nullable
    private String requestAddressFromNominatim(@NonNull GeoPoint point) {
        HttpURLConnection connection = null;
        try {
            String urlValue = String.format(
                    Locale.US,
                    "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%.7f&lon=%.7f&zoom=18&addressdetails=1",
                    point.getLatitude(),
                    point.getLongitude()
            );

            URL url = new URL(urlValue);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "AidBridge-Android/1.0 (contact@aidbridge.local)");

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(builder.toString());
            JSONObject address = json.optJSONObject("address");
            if (address != null) {
                String road = firstNonEmpty(address.optString("road"), address.optString("neighbourhood"), address.optString("suburb"));
                String district = firstNonEmpty(address.optString("city_district"), address.optString("city"), address.optString("state"));
                if (road != null && district != null) {
                    return road + ", " + district;
                }
                if (road != null) {
                    return road;
                }
            }

            String displayName = json.optString("display_name");
            return displayName != null ? displayName : null;
        } catch (Exception exception) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Nullable
    private String firstNonEmpty(@Nullable String first, @Nullable String second, @Nullable String third) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        if (third != null && !third.trim().isEmpty()) {
            return third.trim();
        }
        return null;
    }

    private List<RoutingRequestDto.DangerousZoneDto> buildHardcodedDangerousZones() {
        List<RoutingRequestDto.DangerousZoneDto> zones = new ArrayList<>();

        zones.add(new RoutingRequestDto.DangerousZoneDto(
                "1",
                0,
                new RoutingRequestDto.GeometryDto(
                        "Polygon",
                        createPolygonCoordinates(new double[][] {
                                {106.7, 10.8},
                                {106.71, 10.8},
                                {106.75, 10.81},
                                {106.65, 10.81},
                                {106.7, 10.8}
                        })
                )
        ));

        zones.add(new RoutingRequestDto.DangerousZoneDto(
                "2",
                0,
                new RoutingRequestDto.GeometryDto(
                        "Polygon",
                        createPolygonCoordinates(new double[][] {
                                {106.72, 10.9},
                                {107.0, 10.9},
                                {107.0, 10.81},
                                {106.8, 10.81},
                                {106.72, 10.9}
                        })
                )
        ));

        return zones;
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

    private List<GeoPoint> decodePolyline(@NonNull String encodedPolyline) {
        List<GeoPoint> polylinePoints = new ArrayList<>();
        int index = 0;
        int latitude = 0;
        int longitude = 0;

        try {
            while (index < encodedPolyline.length()) {
                int result = 0;
                int shift = 0;
                int b;
                do {
                    b = encodedPolyline.charAt(index++) - 63;
                    result |= (b & 0x1F) << shift;
                    shift += 5;
                } while (b >= 0x20);

                int deltaLatitude = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
                latitude += deltaLatitude;

                result = 0;
                shift = 0;
                do {
                    b = encodedPolyline.charAt(index++) - 63;
                    result |= (b & 0x1F) << shift;
                    shift += 5;
                } while (b >= 0x20);

                int deltaLongitude = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
                longitude += deltaLongitude;

                polylinePoints.add(new GeoPoint(latitude / 1E5, longitude / 1E5));
            }
        } catch (Exception exception) {
            polylinePoints.clear();
        }

        return polylinePoints;
    }

    private double distanceMeters(@NonNull GeoPoint from, @NonNull GeoPoint to) {
        Location fromLocation = new Location("from");
        fromLocation.setLatitude(from.getLatitude());
        fromLocation.setLongitude(from.getLongitude());

        Location toLocation = new Location("to");
        toLocation.setLatitude(to.getLatitude());
        toLocation.setLongitude(to.getLongitude());

        return fromLocation.distanceTo(toLocation);
    }

    private String formatDistance(double distanceMeters) {
        if (distanceMeters >= 1000d) {
            return getString(R.string.volunteer_map_distance_km, distanceMeters / 1000d);
        }
        return getString(R.string.volunteer_map_distance_m, Math.round(distanceMeters));
    }

    private String formatDuration(long durationSeconds) {
        long hours = durationSeconds / 3600L;
        long minutes = (durationSeconds % 3600L) / 60L;
        long seconds = durationSeconds % 60L;

        if (hours > 0L) {
            return getString(R.string.volunteer_map_duration_hour_min, hours, minutes);
        }
        if (minutes > 0L) {
            return getString(R.string.volunteer_map_duration_min_sec, minutes, seconds);
        }
        return getString(R.string.volunteer_map_duration_seconds, seconds);
    }

    private void showRouteError(@NonNull String message) {
        pendingNavigationAfterRouteCalculation = false;
        showTopSnackbar(binding.getRoot(), message, true);
    }

    @NonNull
    private GeoPoint resolveInitialCenterPoint() {
        if (isWithinRoutingBounds(currentPoint)) {
            return currentPoint;
        }
        if (isWithinRoutingBounds(startPoint)) {
            return startPoint;
        }
        if (isWithinRoutingBounds(endPoint)) {
            return endPoint;
        }
        return new GeoPoint(DEFAULT_END_LAT, DEFAULT_END_LON);
    }

    private boolean isWithinRoutingBounds(@Nullable GeoPoint point) {
        if (point == null) {
            return false;
        }

        double lat = point.getLatitude();
        double lon = point.getLongitude();
        return lat >= ROUTING_MIN_LAT
                && lat <= ROUTING_MAX_LAT
                && lon >= ROUTING_MIN_LON
                && lon <= ROUTING_MAX_LON;
    }
}
