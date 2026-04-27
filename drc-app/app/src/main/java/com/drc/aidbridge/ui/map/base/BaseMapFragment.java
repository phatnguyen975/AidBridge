package com.drc.aidbridge.ui.map.base;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.BuildConfig;
import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.RoutingRequestDto;
import com.drc.aidbridge.data.remote.dto.response.RoutingResponseDto;
import com.drc.aidbridge.data.remote.dto.response.hub.HubDto;
import com.drc.aidbridge.databinding.FragmentMapBaseBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.map.feature.hub.HubSearchDrawerFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import com.drc.aidbridge.ui.map.base.helper.MapOverlayHelper;
import com.drc.aidbridge.ui.map.base.helper.MapMarkerHelper;
import com.drc.aidbridge.ui.map.base.helper.LocationServiceHelper;
import com.drc.aidbridge.ui.map.base.helper.NavigationSimulationHelper;
import com.drc.aidbridge.ui.map.base.helper.HubSearchInteractionHelper;
import com.drc.aidbridge.ui.map.base.helper.RouteProgressHelper;
import com.drc.aidbridge.ui.map.base.helper.TopOverviewPanelHelper;

public abstract class BaseMapFragment<VM extends BaseMapViewModel> extends BaseFragment<FragmentMapBaseBinding> {

    protected static final int LOCATION_PERMISSION_REQUEST_CODE = 7024;
    protected static final double DEFAULT_END_LAT = 11.0;
    protected static final double DEFAULT_END_LON = 106.8;
    protected static final double ROUTING_MIN_LON = 102.126801;
    protected static final double ROUTING_MAX_LON = 114.333212;
    protected static final double ROUTING_MIN_LAT = 7.8917885;
    protected static final double ROUTING_MAX_LAT = 23.3996861;
    protected static final long ROUTE_REQUEST_DEBOUNCE_MS = 1500L;
    protected static final double ROUTE_SMOOTHING_SEGMENT_METERS = 16d;
    protected static final int ROUTE_SMOOTHING_MAX_STEPS = 12;
    protected static final double INSTRUCTION_UPDATE_LOCATION_THRESHOLD_METERS = 10d;
    protected static final int INSTRUCTION_ROUTE_POINT_WINDOW = 140;
    protected static final double INSTRUCTION_ROUTE_FULL_SCAN_THRESHOLD_METERS = 120d;
    protected static final double NAVIGATION_FOLLOW_ZOOM = 19d;
    protected static final long CAMERA_FOLLOW_INTERVAL_MS = 1000L;
    protected static final float TOP_OVERVIEW_COLLAPSED_MAX_RATIO = 0.52f;
    protected static final float TOP_OVERVIEW_HALF_EXPANDED_RATIO = 0.62f;
    protected static final long DEV_TAP_WINDOW_MS = 1500L;

    @Nullable
    protected MapView mapView;

    protected final MapOverlayHelper mapOverlayHelper = new MapOverlayHelper();
    protected final MapMarkerHelper mapMarkerHelper = new MapMarkerHelper();
    protected final LocationServiceHelper locationServiceHelper = new LocationServiceHelper();
    protected final NavigationSimulationHelper navigationSimulationHelper = new NavigationSimulationHelper();
    protected final HubSearchInteractionHelper hubSearchInteractionHelper = new HubSearchInteractionHelper();
    protected final RouteProgressHelper routeProgressHelper = new RouteProgressHelper();
    protected final TopOverviewPanelHelper topOverviewPanelHelper = new TopOverviewPanelHelper();
    protected final com.drc.aidbridge.ui.map.base.helper.DevPanelHelper devPanelHelper = new com.drc.aidbridge.ui.map.base.helper.DevPanelHelper();
    protected final com.drc.aidbridge.ui.map.base.helper.GeocodingHelper geocodingHelper = new com.drc.aidbridge.ui.map.base.helper.GeocodingHelper();
    protected final com.drc.aidbridge.ui.map.base.helper.RouteOptionsHelper routeOptionsHelper = new com.drc.aidbridge.ui.map.base.helper.RouteOptionsHelper();

    protected ExecutorService geocodeExecutor;
    protected final Handler mainHandler = new Handler(Looper.getMainLooper());

    protected GeoPoint startPoint;
    protected GeoPoint endPoint;
    protected GeoPoint currentPoint;

    protected final List<GeoPoint> routePoints = new ArrayList<>();
    protected final List<Double> routeProgressMeters = new ArrayList<>();
    protected List<RoutingResponseDto.InstructionDto> currentInstructions = Collections.emptyList();
    protected List<RoutingRequestDto.DangerousZoneDto> currentDangerousZones = Collections.emptyList();

    protected boolean avoidDangerousZones;
    protected boolean isNavigationActive;
    protected boolean isManualStartPoint;
    protected boolean isNetworkDropSimulated;
    protected boolean isTopOverviewExpanded = false;
    protected boolean isMapScreenActive;
    protected boolean shouldRefreshAutoStartOnNextFix = true;
    protected boolean hasShownOutOfBoundsWarning;
    protected boolean isRoutingLoading;
    protected long lastRouteRequestAtMs;
    protected long lastAvatarTapAt;
    protected int avatarTapCount;
    protected long lastMarkerUpdateAtMs;
    protected static final long MARKER_UPDATE_THROTTLE_MS = 300L;
    protected long lastCameraFollowAtMs;
    protected boolean isMapBaseInitialized;
    protected boolean isMapResumeScheduled;
    protected boolean isCameraFollowEnabled;
    protected boolean hasCenteredOnInitialGps;
    protected boolean pendingNavigationAfterRouteCalculation;
    protected boolean isTopOverviewAutoHiddenForRoute;
    protected int defaultTopOverviewPeekHeightPx;
    protected int minTopOverviewPeekHeightPx;
    protected int currentTopOverviewCollapsedPeekHeightPx;
    protected int topOverviewBaseBottomMarginPx;
    protected int topOverviewBaseFabBottomMarginPx;
    protected int topOverviewBaseRecenterFabBottomMarginPx;
    protected int topOverviewSystemTopInsetPx;
    protected int topOverviewSystemBottomInsetPx;
    @Nullable
    protected BottomSheetBehavior<View> topOverviewBottomSheetBehavior;
    protected int cachedInstructionIndex = -1;
    protected int cachedNearestRoutePointIndex = -1;
    @Nullable
    protected GeoPoint lastInstructionIndexLocation;

    protected enum PointSelectionMode {
        START, END, NONE
    }

    protected PointSelectionMode pointSelectionMode = PointSelectionMode.NONE;

    protected abstract VM getViewModel();

    protected abstract int getContentLayout();

    protected abstract void setupRoleSpecificUI();

    @Override
    protected FragmentMapBaseBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentMapBaseBinding.inflate(inflater, container, false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        
        int childLayoutId = getContentLayout();
        if (childLayoutId != 0) {
            inflater.inflate(childLayoutId, binding.childContentContainer, true);
        }
        
        return rootView;
    }

    @Override
    protected void setupViews() {
        if (geocodeExecutor == null || geocodeExecutor.isShutdown()) {
            geocodeExecutor = Executors.newSingleThreadExecutor();
        }

        geocodingHelper.init();
        geocodingHelper.setListener(new com.drc.aidbridge.ui.map.base.helper.GeocodingHelper.GeocodingListener() {
            @Override
            public void onAddressResolved(@Nullable String resolved, boolean forStart) {
                if (resolved == null || resolved.trim().isEmpty()) {
                    return;
                }

                String addressText = getString(
                        R.string.base_map_address_format,
                        getString(forStart
                                ? R.string.base_map_base_address_label
                                : R.string.base_map_victim_address_label),
                        resolved);

                if (forStart) {
                    getViewModel().setStartAddress(addressText);
                } else {
                    getViewModel().setEndAddress(addressText);
                }

                if (binding == null) {
                    return;
                }

                if (forStart) {
                    binding.tvStartAddress.setText(addressText);
                } else {
                    binding.tvEndAddress.setText(addressText);
                    binding.tvPrimaryDestination.setText(toCompactAddress(addressText));
                }
            }
        });

        devPanelHelper.setListener(new com.drc.aidbridge.ui.map.base.helper.DevPanelHelper.DevPanelListener() {
            @Override
            public void onApplyCustomEndPoint(@NonNull GeoPoint point) {
                endPoint = point;
                getViewModel().setEndPoint(point);
                clearCurrentRoutePathState();
                reverseGeocodeAsync(endPoint, false);
                updateMapMarkers();
            }

            @Override
            public void onScenario2KmRequested() {
                if (startPoint == null) {
                    showRouteError(getString(R.string.base_map_missing_location));
                    return;
                }
                endPoint = new GeoPoint(startPoint.getLatitude() + 0.018, startPoint.getLongitude() + 0.006);
                getViewModel().setEndPoint(endPoint);
                clearCurrentRoutePathState();
                reverseGeocodeAsync(endPoint, false);
                updateMapMarkers();
            }

            @Override
            public void onNetworkDropToggled(boolean isNetworkDropSimulatedState) {
                isNetworkDropSimulated = isNetworkDropSimulatedState;
                getViewModel().setNetworkDropSimulated(isNetworkDropSimulated);
                showToast(isNetworkDropSimulated
                        ? getString(R.string.base_map_network_dropped)
                        : getString(R.string.base_map_route_success));
            }

            @Override
            public void onClearMapRequested() {
                clearMapState();
            }

            @Override
            public void onError(@NonNull String message) {
                showRouteError(message);
            }
        });

        routeOptionsHelper.setListener(new com.drc.aidbridge.ui.map.base.helper.RouteOptionsHelper.RouteOptionsListener() {
            @Override
            public void onApplyOptions(@NonNull String selectedStrategy, boolean avoidDangerous) {
                avoidDangerousZones = avoidDangerous;
                getViewModel().setSelectedStrategy(selectedStrategy);
                getViewModel().setAvoidDangerousZones(avoidDangerousZones);
                updateRouteOptionLabel();
                requestRouteCalculation();
            }
        });

        locationServiceHelper.init(requireActivity(), new com.drc.aidbridge.ui.map.base.helper.LocationServiceHelper.LocationListener() {
            @Override
            public void onLocationChanged(@NonNull org.osmdroid.util.GeoPoint point) {
                applyCurrentLocation(point);
            }

            @Override
            public void onPermissionDenied() {
                showRouteError(getString(R.string.base_map_location_permission_denied));
            }
        });
        locationServiceHelper.setMapScreenActive(true);
        restoreSessionFromViewModel();
        setupMap();
        setupMissionOverviewControls();
        topOverviewPanelHelper.setupTopOverviewBottomSheet(binding, isExpanded -> {
            isTopOverviewExpanded = isExpanded;
            getViewModel().setTopOverviewExpanded(isExpanded);
        });
        setupNavigationControls();
        navigationSimulationHelper.setup(requireActivity(), binding, isNetworkDropSimulated, new com.drc.aidbridge.ui.map.base.helper.NavigationSimulationHelper.SimulationListener() {
            @Override
            public void onSimulationStartRequested() {
                if (getViewModel().isSimulationRunning()) {
                    getViewModel().stopSimulation();
                } else {
                    startSimulation();
                }
            }

            @Override
            public void onSimulationStopRequested() {
                getViewModel().stopSimulation();
            }

            @Override
            public void onApplyCustomEndPoint(@NonNull org.osmdroid.util.GeoPoint customEndPoint) {
                endPoint = customEndPoint;
                getViewModel().setEndPoint(customEndPoint);
                clearCurrentRoutePathState();
                reverseGeocodeAsync(endPoint, false);
                updateMapMarkers();
            }

            @Override
            public void onScenario2KmRequested() {
                if (startPoint != null) {
                    endPoint = new org.osmdroid.util.GeoPoint(startPoint.getLatitude() + 0.018, startPoint.getLongitude() + 0.006);
                    getViewModel().setEndPoint(endPoint);
                    clearCurrentRoutePathState();
                    reverseGeocodeAsync(endPoint, false);
                    updateMapMarkers();
                } else {
                    showRouteError(getString(R.string.base_map_missing_location));
                }
            }

            @Override
            public void onToggleNetworkSimulation() {
                isNetworkDropSimulated = !isNetworkDropSimulated;
                getViewModel().setNetworkDropSimulated(isNetworkDropSimulated);
                showToast(isNetworkDropSimulated
                        ? getString(R.string.base_map_network_dropped)
                        : getString(R.string.base_map_route_success));
            }

            @Override
            public void onClearMapState() {
                clearMapState();
            }
        });
        applyRestoredUiState();
        ensureCurrentLocation();
        
        hubSearchInteractionHelper.setup(this, binding, this::onHubSelected);
        setupRoleSpecificUI();
    }

    @Override
    protected void observeViewModel() {
        getViewModel().getRouteResult().observe(
                getViewLifecycleOwner(),
                resultObserver(this::renderRouteResult, this::showRouteError));

        getViewModel().getSimulatedPoint().observe(getViewLifecycleOwner(), point -> {
            if (point == null) {
                return;
            }
            applySimulatedPoint(point);
        });

        getViewModel().getSimulationState().observe(getViewLifecycleOwner(), this::updateSimulationButtonState);

        getViewModel().getHubSearchResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess() && result.getData() != null) {
                drawHubMarkers(result.getData());
            } else if (result != null && !result.isLoading()) {
                clearHubMarkers();
            }
        });
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
        locationServiceHelper.setMapScreenActive(true);
        shouldRefreshAutoStartOnNextFix = true;
        hasShownOutOfBoundsWarning = false;

        if (isMapResumeScheduled) {
            return;
        }

        if (binding == null) {
            return;
        }

        isMapResumeScheduled = true;

        if (mapView != null) {
            try {
                mapView.setUseDataConnection(true);
                mapView.onResume();
            } catch (Exception e) {
                // Ignore
            }
        }

        mainHandler.postDelayed(() -> {
            if (mapView == null || !isMapScreenActive || binding == null) {
                isMapResumeScheduled = false;
                return;
            }

            try {
                int restoredInstructionIndex = getViewModel().getCachedInstructionIndex();
                if (restoredInstructionIndex >= 0 && restoredInstructionIndex < currentInstructions.size()) {
                    cachedInstructionIndex = restoredInstructionIndex;
                } else {
                    cachedInstructionIndex = -1;
                }

                String cachedPolyline = getViewModel().getLastPolyline();
                if (cachedPolyline != null && !cachedPolyline.trim().isEmpty()) {
                    try {
                        drawRoutePolyline(cachedPolyline, false);
                    } catch (Exception e) {
                        // Ignore
                    }
                }

                try {
                    updateMapMarkersIfNeeded();
                } catch (Exception e) {
                    // Ignore
                }

                try {
                    updateHudInstructionsIfNeeded();
                } catch (Exception e) {
                    // Ignore
                }

                List<RoutingRequestDto.DangerousZoneDto> cachedZones = getViewModel().getCachedDangerousZones();
                if (!cachedZones.isEmpty()) {
                    try {
                        currentDangerousZones = cachedZones;
                        renderDangerousZones(cachedZones);
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            } catch (Exception e) {
                // Ignore
            }

            isMapResumeScheduled = false;
        }, 500L);

        mainHandler.postDelayed(locationServiceHelper::startLocationUpdates, 300L);

        try {
            isNavigationActive = getViewModel().isNavigationActive();
            if (isNavigationActive) {
                enterNavigationMode();
            }

            getViewModel().resumeSimulationIfNeeded();
            if (getViewModel().isSimulationRunning()) {
                updateSimulationButtonState(Boolean.TRUE);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    public void onPause() {
        isMapScreenActive = false;
        locationServiceHelper.setMapScreenActive(false);
        isMapResumeScheduled = false;

        getViewModel().setCachedInstructionIndex(cachedInstructionIndex);
        getViewModel().setLastSimulationPointBeforePause(getViewModel().getSimulationPointIndex());
        getViewModel().setCachedDangerousZones(currentDangerousZones);

        locationServiceHelper.stopLocationUpdates();
        getViewModel().pauseSimulationForBackground();
        if (mapView != null) {
            mapView.setUseDataConnection(false);
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) {
            try {
                mapOverlayHelper.detach();
                mapMarkerHelper.detach();
                mapView.getOverlays().clear();

                mapView.setUseDataConnection(false);
                mapView.onPause();
                mapView.onDetach();
            } catch (Exception e) {
                // Ignore
            }
        }

        lastMarkerUpdateAtMs = 0;
        routeProgressMeters.clear();
        cachedNearestRoutePointIndex = -1;
        topOverviewBottomSheetBehavior = null;

        locationServiceHelper.detach();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (geocodeExecutor != null) {
            geocodeExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    protected void restoreSessionFromViewModel() {
        avoidDangerousZones = getViewModel().isAvoidDangerousZones();
        isNavigationActive = getViewModel().isNavigationActive();
        isManualStartPoint = getViewModel().isManualStartPoint();
        isNetworkDropSimulated = getViewModel().isNetworkDropSimulated();
        isTopOverviewExpanded = false;

        startPoint = getViewModel().getStartPoint();
        endPoint = getViewModel().getEndPoint();
        currentPoint = getViewModel().getCurrentPoint();

        routePoints.clear();
        routePoints.addAll(getViewModel().getRoutePoints());
        currentInstructions = getViewModel().getInstructions();
        routeProgressHelper.setRouteData(routePoints, currentInstructions);
        cachedNearestRoutePointIndex = -1;
    }

    protected void applyRestoredUiState() {
        updateRouteOptionLabel();
        applyTopOverviewState();

        String cachedStartAddress = getViewModel().getStartAddress();
        if (cachedStartAddress != null && !cachedStartAddress.trim().isEmpty()) {
            binding.tvStartAddress.setText(cachedStartAddress);
        }

        String cachedEndAddress = getViewModel().getEndAddress();
        if (cachedEndAddress != null && !cachedEndAddress.trim().isEmpty()) {
            binding.tvEndAddress.setText(cachedEndAddress);
            binding.tvPrimaryDestination.setText(toCompactAddress(cachedEndAddress));
        } else {
            binding.tvPrimaryDestination.setText(R.string.base_map_end_address_loading_short);
        }

        Double cachedDistance = getViewModel().getLastDistanceMeters();
        if (cachedDistance != null) {
            binding.tvRouteDistance.setText(formatDistance(cachedDistance));
        }

        Long cachedDuration = getViewModel().getLastDurationSeconds();
        if (cachedDuration != null) {
            binding.tvRouteDuration.setText(buildRouteDurationLabel(cachedDuration, getViewModel().getLastRouteSource()));
        }

        if (isNavigationActive) {
            enterNavigationMode();
        } else {
            exitNavigationMode();
        }
    }

    protected void setupMap() {
        mapView = binding.osmMapView;
        if (mapView != null) {
            mapOverlayHelper.attach(mapView);
            mapMarkerHelper.attach(mapView);
        }
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        MapController controller = (MapController) mapView.getController();
        controller.setZoom(14.2d);
        controller.setCenter(resolveInitialCenterPoint());

        mapView.setOnTouchListener((v, event) -> {
            int actionMasked = event.getActionMasked();
            if (actionMasked == MotionEvent.ACTION_MOVE || actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                disableCameraFollowOnManualMapInteraction();
            }

            if (event.getAction() == MotionEvent.ACTION_UP && pointSelectionMode != PointSelectionMode.NONE) {
                handleMapTap(event);
            }

            return false;
        });

        isMapBaseInitialized = true;
    }

    protected void setupMissionOverviewControls() {
        binding.btnToggleTopPanel.setOnClickListener(v -> toggleTopOverviewPanel());
        binding.btnRouteOptions.setOnClickListener(v -> showRouteOptionBottomSheet());
        binding.btnCalculateRoute.setOnClickListener(v -> {
            hideTopOverviewPanelForRouteFocus();
            requestRouteCalculation();
        });
        binding.btnStartNavigation.setOnClickListener(v -> handleStartNavigationClick());
        binding.btnCallVictim.setOnClickListener(v -> openQuickDial());
        binding.fabOpenControlPanel.setOnClickListener(v -> openTopOverviewPanelFromFab());
        binding.fabRecenterCurrentLocation.setOnClickListener(v -> recenterMapToCurrentLocation());

        binding.btnSetStartPoint.setOnClickListener(v -> showStartPointModeMenu());
        binding.btnSetEndPoint.setOnClickListener(v -> setPointSelectionMode(PointSelectionMode.END));

        binding.tvDevHintSign.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - lastAvatarTapAt > DEV_TAP_WINDOW_MS) {
                avatarTapCount = 0;
            }
            lastAvatarTapAt = now;
            avatarTapCount++;
            if (avatarTapCount >= 5) {
                avatarTapCount = 0;
                showDevPanelBottomSheet();
            }
        });

        updatePointSelectionButtonStates();
        updateOpenControlFabVisibility();
    }

    protected void showStartPointModeMenu() {
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
            showToast(getString(R.string.base_map_pick_start_on_map_hint));
            dialog.dismiss();
        });

        currentCard.setOnClickListener(v -> {
            setStartPointToCurrentLocation();
            dialog.dismiss();
        });

        dialog.show();
    }

    protected void setStartPointToCurrentLocation() {
        if (currentPoint == null) {
            showRouteError(getString(R.string.base_map_missing_location));
            return;
        }

        if (!isWithinRoutingBounds(currentPoint)) {
            showRouteError(getString(R.string.base_map_location_out_of_bounds));
            return;
        }

        startPoint = new GeoPoint(currentPoint.getLatitude(), currentPoint.getLongitude());
        getViewModel().setStartPoint(startPoint);
        isManualStartPoint = true;
        shouldRefreshAutoStartOnNextFix = false;
        getViewModel().setManualStartPoint(true);

        clearCurrentRoutePathState();
        reverseGeocodeAsync(startPoint, true);
        updateMapMarkers();
        showToast(getString(R.string.base_map_start_set_to_current));
    }

    protected void handleStartNavigationClick() {
        if (startPoint == null) {
            if (currentPoint == null || !isWithinRoutingBounds(currentPoint)) {
                showRouteError(getString(R.string.base_map_missing_location));
                return;
            }

            startPoint = new GeoPoint(currentPoint.getLatitude(), currentPoint.getLongitude());
            getViewModel().setStartPoint(startPoint);
            isManualStartPoint = true;
            getViewModel().setManualStartPoint(true);
            shouldRefreshAutoStartOnNextFix = false;
            reverseGeocodeAsync(startPoint, true);
        }

        if (endPoint == null) {
            showRouteError(getString(R.string.base_map_no_target_location));
            return;
        }

        if (!isWithinRoutingBounds(startPoint) || !isWithinRoutingBounds(endPoint)) {
            showRouteError(getString(R.string.base_map_route_out_of_bounds));
            return;
        }

        pendingNavigationAfterRouteCalculation = true;
        requestRouteCalculation(true);
    }



    protected void openTopOverviewPanelFromFab() {
        topOverviewPanelHelper.openTopOverviewPanelFromFab(binding);
    }

    protected void updateOpenControlFabVisibility() {
        topOverviewPanelHelper.updateOpenControlFabVisibility(binding);
    }
    protected void ensureControlPanelAvailable() {
        topOverviewPanelHelper.ensureControlPanelAvailable(binding);
    }

    protected void recenterMapToCurrentLocation() {
        GeoPoint targetPoint = currentPoint;
        if (targetPoint == null) {
            showRouteError(getString(R.string.base_map_missing_location));
            return;
        }

        if (isNavigationActive || (getViewModel() != null && getViewModel().isSimulationRunning())) {
            isCameraFollowEnabled = true;
            lastCameraFollowAtMs = System.currentTimeMillis();
        }

        focusCameraOnPoint(targetPoint);
    }

    protected void hideTopOverviewPanelForRouteFocus() {
        topOverviewPanelHelper.setTopOverviewAutoHiddenForRoute(binding, true);
    }

    protected void setPointSelectionMode(PointSelectionMode mode) {
        pointSelectionMode = mode;
        updatePointSelectionButtonStates();
    }

    protected void updatePointSelectionButtonStates() {
        try {
            if (binding == null)
                return;

            boolean isSelectingStart = pointSelectionMode == PointSelectionMode.START;
            boolean isSelectingEnd = pointSelectionMode == PointSelectionMode.END;

            binding.btnSetStartPoint.animate()
                .scaleX(isSelectingStart ? 1.3f : 1.0f)
                .scaleY(isSelectingStart ? 1.3f : 1.0f)
                .alpha(isSelectingStart ? 1.0f : 0.7f)
                .setDuration(200)
                .start();

            binding.btnSetEndPoint.animate()
                .scaleX(isSelectingEnd ? 1.3f : 1.0f)
                .scaleY(isSelectingEnd ? 1.3f : 1.0f)
                .alpha(isSelectingEnd ? 1.0f : 0.7f)
                .setDuration(200)
                .start();
        } catch (Exception e) {
            // Ignore
        }
    }

    protected void setupNavigationControls() {
        binding.btnCancelMission.setOnClickListener(v -> {
            if (getViewModel().isSimulationRunning()) {
                getViewModel().stopSimulation();
            }
            exitNavigationMode();
            showToast(getString(R.string.base_map_mission_cancelled));
        });

        binding.btnArrivedMission.setOnClickListener(v -> {
            if (getViewModel().isSimulationRunning()) {
                getViewModel().stopSimulation();
            }
            showToast(getString(R.string.base_map_arrived_success));
            exitNavigationMode();
        });
    }



    protected void ensureCurrentLocation() {
        locationServiceHelper.ensureLocation(requireActivity());
    }

    protected boolean hasLocationPermission() {
        return locationServiceHelper.hasLocationPermission(requireContext());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationServiceHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    protected void applyCurrentLocation(@NonNull GeoPoint geoPoint) {
        if (getViewModel() != null && getViewModel().isSimulationRunning()) {
            return;
        }
        currentPoint = geoPoint;
        getViewModel().setCurrentPoint(geoPoint);
        centerOnCurrentLocationFirstTimeIfNeeded(geoPoint);

        if (!isManualStartPoint && !isNavigationActive && shouldRefreshAutoStartOnNextFix) {
            shouldRefreshAutoStartOnNextFix = false;
            if (!isWithinRoutingBounds(geoPoint)) {
                if (!hasShownOutOfBoundsWarning) {
                    hasShownOutOfBoundsWarning = true;
                    showRouteError(getString(R.string.base_map_location_out_of_bounds));
                }
                return;
            }

            hasShownOutOfBoundsWarning = false;
            boolean startChanged = startPoint == null || distanceMeters(startPoint, geoPoint) > 20d;
            if (startChanged) {
                startPoint = geoPoint;
                getViewModel().setStartPoint(geoPoint);
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

        maybeFollowCurrentLocation();
        updateMapMarkersThrottled();
        updateHudInstructions();
    }

    protected void centerOnCurrentLocationFirstTimeIfNeeded(@NonNull GeoPoint locationPoint) {
        if (hasCenteredOnInitialGps || mapView == null || !isWithinRoutingBounds(locationPoint)) {
            return;
        }

        try {
            mapView.getController().animateTo(locationPoint);
            hasCenteredOnInitialGps = true;
        } catch (Exception e) {
            // Ignore
        }
    }

    protected void requestRouteCalculation() {
        requestRouteCalculation(false);
    }

    protected void requestRouteCalculation(boolean bypassDebounce) {
        if (isRoutingLoading) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!bypassDebounce && now - lastRouteRequestAtMs < ROUTE_REQUEST_DEBOUNCE_MS) {
            return;
        }

        if (isNetworkDropSimulated) {
            showRouteError(getString(R.string.base_map_network_dropped));
            return;
        }

        if (startPoint == null) {
            showRouteError(getString(R.string.base_map_missing_location));
            return;
        }

        if (endPoint == null) {
            showRouteError(getString(R.string.base_map_no_target_location));
            return;
        }

        if (!isWithinRoutingBounds(startPoint) || !isWithinRoutingBounds(endPoint)) {
            showRouteError(getString(R.string.base_map_route_out_of_bounds));
            return;
        }

        RoutingRequestDto requestDto = getViewModel().createRoutingRequest(startPoint, endPoint);

        lastRouteRequestAtMs = now;
        renderDangerousZones(requestDto.getDangerousZones());
        getViewModel().calculateRoute(requestDto);
    }

    protected void renderRouteResult(@Nullable RoutingResponseDto response) {
        if (response == null) {
            showRouteError(getString(R.string.base_map_route_failed));
            return;
        }

        currentInstructions = response.getInstructions() != null
                ? response.getInstructions()
                : Collections.emptyList();
        getViewModel().setInstructions(currentInstructions);
        routeProgressHelper.setRouteData(routePoints, currentInstructions);

        cachedInstructionIndex = -1;
        cachedNearestRoutePointIndex = -1;
        lastInstructionIndexLocation = null;

        double distance = response.getDistance() != null ? response.getDistance() : 0d;
        long duration = response.getDuration() != null ? response.getDuration() : 0L;
        String routeSource = response.getRouteSource();
        getViewModel().setLastDistanceMeters(distance);
        getViewModel().setLastDurationSeconds(duration);
        getViewModel().setLastRouteSource(routeSource);
        binding.tvRouteDistance.setText(formatDistance(distance));
        binding.tvRouteDuration.setText(buildRouteDurationLabel(duration, routeSource));

        drawRoutePolyline(response.getPolyline(), true);
        updateMapMarkers();
        updateHudInstructions();

        if (pendingNavigationAfterRouteCalculation) {
            pendingNavigationAfterRouteCalculation = false;
            enterNavigationMode();
            return;
        }

        if (!isNavigationActive) {
            showToast(getString(R.string.base_map_route_success));
        }
    }

    protected void drawRoutePolyline(@Nullable String encodedPolyline, boolean cachePolyline) {
        routePoints.clear();
        clearRouteOverlays();

        if (encodedPolyline == null || encodedPolyline.trim().isEmpty()) {
            showRouteError(getString(R.string.base_map_route_missing_polyline));
            mapView.invalidate();
            if (cachePolyline) {
                getViewModel().setLastPolyline(null);
                getViewModel().setRoutePoints(Collections.emptyList());
            }
            return;
        }

        List<GeoPoint> decodedPoints = mapOverlayHelper.decodePolyline(encodedPolyline);
        if (decodedPoints.isEmpty()) {
            showRouteError(getString(R.string.base_map_route_missing_polyline));
            mapView.invalidate();
            return;
        }

        List<GeoPoint> smoothedPoints = mapOverlayHelper.smoothRoutePoints(
                decodedPoints,
                ROUTE_SMOOTHING_SEGMENT_METERS,
                ROUTE_SMOOTHING_MAX_STEPS);
        routePoints.addAll(smoothedPoints);
        routeProgressHelper.setRouteData(routePoints, currentInstructions);
        cachedNearestRoutePointIndex = -1;

        drawRouteOverlays(
                smoothedPoints,
                R.color.base_map_route_casing,
                R.dimen.volunteer_map_route_casing_stroke_width,
                R.color.base_map_route_core,
                R.dimen.volunteer_map_route_stroke_width,
                R.dimen.spacing_xl);

        if (cachePolyline) {
            getViewModel().setLastPolyline(encodedPolyline);
            getViewModel().setRoutePoints(smoothedPoints);
        }
    }

    protected void updateMapMarkers() {
        GeoPoint dynamicPoint = currentPoint != null ? currentPoint : startPoint;
        updateStandardMarkers(
                startPoint,
                endPoint,
                dynamicPoint,
                getString(R.string.base_map_start_label),
                getString(R.string.base_map_end_label),
                getString(R.string.base_map_base_address_label),
                R.drawable.bg_map_start_marker,
                R.drawable.bg_map_end_marker,
                R.drawable.bg_map_current_marker,
                5d,
                5d,
                3d);
    }

    protected void updateMapMarkersIfNeeded() {
        if (mapView == null) {
            return;
        }

        if (!mapMarkerHelper.hasStartMarker() || !mapMarkerHelper.hasEndMarker() || !mapMarkerHelper.hasCurrentMarker()) {
            updateMapMarkersThrottled();
        }
    }

    protected void updateMapMarkersThrottled() {
        long now = System.currentTimeMillis();
        if (now - lastMarkerUpdateAtMs < MARKER_UPDATE_THROTTLE_MS) {
            return;
        }
        lastMarkerUpdateAtMs = now;
        updateMapMarkers();
    }

    protected void updateHudInstructionsIfNeeded() {
        if (binding == null || !isNavigationActive || currentInstructions.isEmpty()) {
            return;
        }

        try {
            int newIndex = resolveInstructionIndex();
            if (newIndex != cachedInstructionIndex) {
                cachedInstructionIndex = newIndex;
                updateHudInstructions();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    protected void renderDangerousZones(@NonNull List<RoutingRequestDto.DangerousZoneDto> zones) {
        currentDangerousZones = new ArrayList<>(zones);
        getViewModel().setCachedDangerousZones(zones);
        renderDangerousZoneOverlays(
                zones,
                R.color.base_map_polygon_fill,
                R.color.base_map_polygon_stroke,
                R.dimen.volunteer_map_polygon_stroke_width);
    }

    protected void enterNavigationMode() {
        isNavigationActive = true;
        getViewModel().setNavigationActive(true);

        if (binding == null) {
            return;
        }

        try {
            binding.cardTopOverview.setVisibility(View.GONE);
            binding.cardNavigationHud.setVisibility(View.VISIBLE);
            binding.layoutNavigationActions.setVisibility(View.VISIBLE);
            updateHudInstructions();
            GeoPoint startFocusPoint = resolveStartFocusPoint();
            if (startFocusPoint != null) {
                focusCameraOnPoint(startFocusPoint);
            }
            updateOpenControlFabVisibility();
        } catch (Exception e) {
            // Ignore
        }
    }

    protected void exitNavigationMode() {
        isNavigationActive = false;
        getViewModel().setNavigationActive(false);
        isCameraFollowEnabled = false;
        lastCameraFollowAtMs = 0L;

        if (binding == null) {
            return;
        }

        try {
            binding.cardTopOverview.setVisibility(
                    isTopOverviewAutoHiddenForRoute ? View.GONE : View.VISIBLE);
            binding.cardNavigationHud.setVisibility(View.GONE);
            binding.layoutNavigationActions.setVisibility(View.GONE);
            if (!isTopOverviewAutoHiddenForRoute) {
                applyTopOverviewState();
            }
            updateOpenControlFabVisibility();
        } catch (Exception e) {
            // Ignore
        }
    }

    protected void toggleTopOverviewPanel() {
        topOverviewPanelHelper.toggleTopOverviewPanel(binding);
    }

    protected void applyTopOverviewState() {
        topOverviewPanelHelper.applyTopOverviewState(binding);
    }

    protected void updateHudInstructions() {
        if (binding == null || !isNavigationActive) {
            return;
        }

        try {
            if (currentInstructions.isEmpty()) {
                binding.tvHudPrimaryInstruction.setText(R.string.base_map_instruction_continue);
                binding.tvHudSecondaryInstruction.setText(R.string.base_map_hud_waiting);
                return;
            }

            int currentIndex = resolveInstructionIndex();

            if (currentIndex < 0 || currentIndex >= currentInstructions.size()) {
                binding.tvHudPrimaryInstruction.setText(R.string.base_map_instruction_continue);
                binding.tvHudSecondaryInstruction.setText(R.string.base_map_hud_waiting);
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

            binding.tvHudSecondaryInstruction.setText(R.string.base_map_hud_waiting);
        } catch (Exception e) {
            if (binding != null) {
                binding.tvHudPrimaryInstruction.setText(R.string.base_map_instruction_continue);
                binding.tvHudSecondaryInstruction.setText(R.string.base_map_hud_waiting);
            }
        }
    }

    protected int resolveInstructionIndex() {
        return routeProgressHelper.resolveInstructionIndex(currentPoint, startPoint);
    }

    protected int findNextDistinctInstructionIndex(int currentIndex) {
        return routeProgressHelper.findNextDistinctInstructionIndex(currentIndex, requireContext());
    }

    protected int findNearestRoutePointIndex(@NonNull GeoPoint reference) {
        return routeProgressHelper.findNearestRoutePointIndex(reference);
    }

    protected double resolveRemainingDistanceForInstruction(int instructionIndex) {
        return routeProgressHelper.resolveRemainingDistanceForInstruction(instructionIndex, currentPoint, startPoint);
    }

    protected void rebuildRouteProgressCache() {
        routeProgressHelper.rebuildRouteProgressCache();
    }

    protected String buildInstructionLabel(@Nullable RoutingResponseDto.InstructionDto instruction) {
        return routeProgressHelper.buildInstructionLabel(instruction, -1d, requireContext());
    }

    protected String buildInstructionLabel(
            @Nullable RoutingResponseDto.InstructionDto instruction,
            double overrideDistanceMeters) {
        return routeProgressHelper.buildInstructionLabel(instruction, overrideDistanceMeters, requireContext());
    }

    protected String buildCommandWithRoad(@Nullable RoutingResponseDto.InstructionDto instruction) {
        return routeProgressHelper.buildCommandWithRoad(instruction, requireContext());
    }

    protected String formatDistance(double distanceMeters) {
        return routeProgressHelper.formatDistance(distanceMeters, requireContext());
    }

    protected void openQuickDial() {
        // To be implemented or overridden if role specific, but we can provide default
    }

    protected void showRouteOptionBottomSheet() {
        routeOptionsHelper.showRouteOptionBottomSheet(requireContext(), getViewModel().getSelectedStrategy(), avoidDangerousZones);
    }

    protected void updateRouteOptionLabel() {
        String selectedStrategy = getViewModel().getSelectedStrategy();
        String strategyLabel;
        switch (selectedStrategy) {
            case BaseMapViewModel.STRATEGY_URGENT:
                strategyLabel = getString(R.string.base_map_strategy_urgent);
                break;
            case BaseMapViewModel.STRATEGY_SAFE:
                strategyLabel = getString(R.string.base_map_strategy_safe);
                break;
            case BaseMapViewModel.STRATEGY_HEAVY_AID:
                strategyLabel = getString(R.string.base_map_strategy_heavy_aid);
                break;
            case BaseMapViewModel.STRATEGY_COMMUNITY:
                strategyLabel = getString(R.string.base_map_strategy_community);
                break;
            default:
                strategyLabel = getString(R.string.base_map_strategy_offroad);
                break;
        }

        String suffix = avoidDangerousZones ? " - " + getString(R.string.base_map_avoid_dangerous) : "";
        binding.btnRouteOptions
                .setText(getString(R.string.base_map_route_options) + ": " + strategyLabel + suffix);
    }

    protected void showDevPanelBottomSheet() {
        devPanelHelper.showDevPanelBottomSheet(requireContext(), isNetworkDropSimulated);
    }

    @Nullable
    protected GeoPoint parseCoordinateInput(@Nullable String input) {
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

    protected void clearMapState() {
        mapOverlayHelper.clearRouteOverlays();
        mapOverlayHelper.clearDangerousZoneOverlays();
        routePoints.clear();
        routeProgressMeters.clear();
        currentInstructions = Collections.emptyList();
        currentDangerousZones = Collections.emptyList();
        cachedInstructionIndex = -1;
        cachedNearestRoutePointIndex = -1;
        lastInstructionIndexLocation = null;

        getViewModel().stopSimulation();
        getViewModel().clearRouteData();
        getViewModel().setInstructions(Collections.emptyList());
        getViewModel().setRoutePoints(Collections.emptyList());
        getViewModel().setCachedDangerousZones(Collections.emptyList());

        endPoint = null;
        getViewModel().setEndPoint(null);
        updateMapMarkers();

        binding.tvRouteDistance.setText(R.string.base_map_distance_default);
        binding.tvRouteDuration.setText(R.string.base_map_duration_default);
        binding.tvHudPrimaryInstruction.setText(R.string.base_map_instruction_continue);
        binding.tvHudSecondaryInstruction.setText(R.string.base_map_hud_waiting);
    }

    protected void clearCurrentRoutePathState() {
        if (mapView == null) {
            return;
        }

        mapOverlayHelper.clearRouteOverlays();
        mapOverlayHelper.clearDangerousZoneOverlays();
        routePoints.clear();
        routeProgressMeters.clear();
        currentInstructions = Collections.emptyList();
        currentDangerousZones = Collections.emptyList();
        cachedInstructionIndex = -1;
        cachedNearestRoutePointIndex = -1;
        lastInstructionIndexLocation = null;
        pendingNavigationAfterRouteCalculation = false;

        getViewModel().setLastPolyline(null);
        getViewModel().setRoutePoints(Collections.emptyList());
        getViewModel().setInstructions(Collections.emptyList());
        getViewModel().setCachedDangerousZones(Collections.emptyList());
        getViewModel().setLastDistanceMeters(null);
        getViewModel().setLastDurationSeconds(null);

        if (binding != null) {
            binding.tvRouteDistance.setText(R.string.base_map_distance_default);
            binding.tvRouteDuration.setText(R.string.base_map_duration_default);
            if (!isNavigationActive) {
                binding.tvHudPrimaryInstruction.setText(R.string.base_map_instruction_continue);
                binding.tvHudSecondaryInstruction.setText(R.string.base_map_hud_waiting);
            }
        }

        mapView.invalidate();
    }

    protected void handleMapTap(@NonNull MotionEvent event) {
        if (mapView == null || pointSelectionMode == PointSelectionMode.NONE) {
            return;
        }

        try {
            GeoPoint tapPoint = (GeoPoint) mapView.getProjection()
                    .fromPixels((int) event.getX(), (int) event.getY());

            if (tapPoint == null) {
                return;
            }

            if (!isWithinRoutingBounds(tapPoint)) {
                showToast(getString(R.string.base_map_route_out_of_bounds));
                return;
            }

            if (pointSelectionMode == PointSelectionMode.START) {
                startPoint = tapPoint;
                getViewModel().setStartPoint(tapPoint);
                isManualStartPoint = true;
                getViewModel().setManualStartPoint(true);
                clearCurrentRoutePathState();
                reverseGeocodeAsync(tapPoint, true);
                showToast(getString(R.string.base_map_point_start_set));
            } else if (pointSelectionMode == PointSelectionMode.END) {
                endPoint = tapPoint;
                getViewModel().setEndPoint(tapPoint);
                clearCurrentRoutePathState();
                reverseGeocodeAsync(tapPoint, false);
                showToast(getString(R.string.base_map_point_end_set));

                getViewModel().setHubSearchPingPoint(tapPoint);
            }

            updateMapMarkers();
            pointSelectionMode = PointSelectionMode.NONE;
            updatePointSelectionButtonStates();
        } catch (Exception e) {
            // Ignore
        }
    }

    protected void startSimulation() {
        if (!BuildConfig.DEBUG || routePoints.isEmpty()) {
            showRouteError(getString(R.string.base_map_no_route_for_simulation));
            return;
        }

        if (!isNavigationActive) {
            enterNavigationMode();
        }
        getViewModel().startSimulationFromCurrentRoute();
        GeoPoint startFocusPoint = resolveStartFocusPoint();
        if (startFocusPoint != null) {
            focusCameraOnPoint(startFocusPoint);
        }
    }

    protected void applySimulatedPoint(@NonNull GeoPoint point) {
        currentPoint = point;
        getViewModel().setCurrentPoint(point);

        if (!isMapScreenActive) {
            return;
        }

        maybeFollowCurrentLocation();
        updateMapMarkersThrottled();
        updateHudInstructions();
    }

    protected void updateSimulationButtonState(@Nullable Boolean isRunning) {
        boolean running = isRunning != null && isRunning;
        if (!running && !isNavigationActive) {
            isCameraFollowEnabled = false;
            lastCameraFollowAtMs = 0L;
        }
        if (!BuildConfig.DEBUG || binding == null) {
            return;
        }
        binding.btnDevSimulate.setText(running
                ? R.string.base_map_dev_simulate_stop
                : R.string.base_map_dev_simulate);
        updateOpenControlFabVisibility();
    }

    protected void reverseGeocodeAsync(@NonNull GeoPoint point, boolean forStart) {
        geocodingHelper.reverseGeocodeAsync(point, forStart);
    }

    @NonNull
    protected String toCompactAddress(@Nullable String addressText) {
        return geocodingHelper.toCompactAddress(addressText, requireContext(), R.string.base_map_end_address_loading_short);
    }



    protected String formatDuration(long durationSeconds) {
        long hours = durationSeconds / 3600L;
        long minutes = (durationSeconds % 3600L) / 60L;
        long seconds = durationSeconds % 60L;

        if (hours > 0L) {
            return getString(R.string.base_map_duration_hour_min, hours, minutes);
        }
        if (minutes > 0L) {
            return getString(R.string.base_map_duration_min_sec, minutes, seconds);
        }
        return getString(R.string.base_map_duration_seconds, seconds);
    }

    @NonNull
    protected String buildRouteDurationLabel(long durationSeconds, @Nullable String routeSource) {
        return getString(
                R.string.base_map_duration_with_source,
                formatDuration(durationSeconds),
                resolveRouteSourceLabel(routeSource));
    }

    @NonNull
    protected String resolveRouteSourceLabel(@Nullable String routeSource) {
        if (RoutingResponseDto.ROUTE_SOURCE_OFFLINE.equalsIgnoreCase(routeSource)) {
            return getString(R.string.base_map_route_source_offline);
        }
        if (RoutingResponseDto.ROUTE_SOURCE_ONLINE.equalsIgnoreCase(routeSource)) {
            return getString(R.string.base_map_route_source_online);
        }
        return getString(R.string.base_map_route_source_unknown);
    }

    protected void showRouteError(@NonNull String message) {
        pendingNavigationAfterRouteCalculation = false;
        showTopSnackbar(binding.getRoot(), message, true);
    }

    @NonNull
    protected GeoPoint resolveInitialCenterPoint() {
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

    protected boolean isWithinRoutingBounds(@Nullable GeoPoint point) {
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

    protected void disableCameraFollowOnManualMapInteraction() {
        if (!isCameraFollowEnabled) {
            return;
        }

        isCameraFollowEnabled = false;
        lastCameraFollowAtMs = 0L;
    }

    protected void maybeFollowCurrentLocation() {
        if (!isCameraFollowEnabled || mapView == null || currentPoint == null) {
            return;
        }

        boolean isSimulationRunning = getViewModel() != null && getViewModel().isSimulationRunning();
        if (!isNavigationActive && !isSimulationRunning) {
            return;
        }

        long now = System.currentTimeMillis();
        if (lastCameraFollowAtMs > 0L && now - lastCameraFollowAtMs < CAMERA_FOLLOW_INTERVAL_MS) {
            return;
        }

        lastCameraFollowAtMs = now;
        focusCameraOnPoint(currentPoint);
    }

    protected void focusCameraOnPoint(@NonNull GeoPoint targetPoint) {
        if (mapView == null) {
            return;
        }

        try {
            mapView.getController().setZoom(NAVIGATION_FOLLOW_ZOOM);
            mapView.getController().animateTo(targetPoint);
        } catch (Exception e) {
            // Ignore
        }
    }

    @Nullable
    protected GeoPoint resolveStartFocusPoint() {
        if (isWithinRoutingBounds(startPoint)) {
            return startPoint;
        }
        if (!routePoints.isEmpty()) {
            return routePoints.get(0);
        }
        if (isWithinRoutingBounds(currentPoint)) {
            return currentPoint;
        }
        return null;
    }

    protected void initializeBaseMap(@NonNull GeoPoint initialCenter,
                                     @NonNull View.OnTouchListener touchListener) {
        mapView = binding.osmMapView;
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        MapController controller = (MapController) mapView.getController();
        controller.setZoom(14.2d);
        controller.setCenter(initialCenter);

        mapView.setOnTouchListener(touchListener);
    }

    protected void clearRouteOverlays() {
        mapOverlayHelper.clearRouteOverlays();
    }

    protected void drawRouteOverlays(@NonNull List<GeoPoint> points,
                                     @ColorRes int casingColorRes,
                                     @DimenRes int casingWidthRes,
                                     @ColorRes int coreColorRes,
                                     @DimenRes int coreWidthRes,
                                     @DimenRes int fitPaddingRes) {
        mapOverlayHelper.drawRouteOverlays(requireContext(), points, casingColorRes, casingWidthRes, coreColorRes, coreWidthRes, fitPaddingRes);
    }

    protected void updateStandardMarkers(@Nullable GeoPoint startPoint,
                                         @Nullable GeoPoint endPoint,
                                         @Nullable GeoPoint dynamicPoint,
                                         @NonNull String startTitle,
                                         @NonNull String endTitle,
                                         @NonNull String dynamicTitle,
                                         @DrawableRes int startIcon,
                                         @DrawableRes int endIcon,
                                         @DrawableRes int dynamicIcon,
                                         double startUpdateThresholdMeters,
                                         double endUpdateThresholdMeters,
                                         double dynamicUpdateThresholdMeters) {
        mapMarkerHelper.updateStandardMarkers(requireContext(), startPoint, endPoint, dynamicPoint, startTitle, endTitle, dynamicTitle, startIcon, endIcon, dynamicIcon, startUpdateThresholdMeters, endUpdateThresholdMeters, dynamicUpdateThresholdMeters);
    }

    protected void renderDangerousZoneOverlays(@NonNull List<RoutingRequestDto.DangerousZoneDto> zones,
                                               @ColorRes int fillColorRes,
                                               @ColorRes int strokeColorRes,
                                               @DimenRes int strokeWidthRes) {
        mapOverlayHelper.renderDangerousZoneOverlays(requireContext(), zones, fillColorRes, strokeColorRes, strokeWidthRes);
    }

    protected void clearDangerousZoneOverlays() {
        mapOverlayHelper.clearDangerousZoneOverlays();
    }

    @NonNull
    protected List<GeoPoint> decodePolyline(@NonNull String encodedPolyline) {
        return mapOverlayHelper.decodePolyline(encodedPolyline);
    }

    @NonNull
    protected List<GeoPoint> smoothRoutePoints(@NonNull List<GeoPoint> originalPoints,
                                               double smoothingSegmentMeters,
                                               int smoothingMaxSteps) {
        return mapOverlayHelper.smoothRoutePoints(originalPoints, smoothingSegmentMeters, smoothingMaxSteps);
    }

    protected void setupHubSearchDrawer() {
        hubSearchInteractionHelper.setup(this, binding, this::onHubSelected);
    }

    protected void onHubSelected(HubDto hub) {
        if (getViewModel().getRoutePoints() != null && !getViewModel().getRoutePoints().isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Xác nhận đổi lộ trình")
                    .setMessage("Bạn hiện đang có một lộ trình trên bản đồ. Thay đổi điểm đích sang Hub '"
                            + hub.getName() + "' sẽ xóa bỏ lộ trình cũ. Bạn có đồng ý không?")
                    .setPositiveButton("Đổi điểm đích", (dialog, which) -> {
                        setHubAsEndpoint(hub);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            setHubAsEndpoint(hub);
        }
    }

    protected void clearHubMarkers() {
        mapMarkerHelper.clearHubMarkers();
    }

    protected void drawHubMarkers(List<HubDto> hubs) {
        mapMarkerHelper.drawHubMarkers(requireContext(), hubs, this::onHubSelected);
    }

    protected void setHubAsEndpoint(HubDto hub) {
        if (binding == null)
            return;
        
        DrawerLayout drawerLayout = binding.drawerLayout;
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        
        if (hub.getLocation() != null) {
            GeoPoint hubPoint = new GeoPoint(hub.getLocation().getLat(), hub.getLocation().getLng());
            getViewModel().setEndPoint(hubPoint);
            getViewModel().setEndAddress(hub.getAddress() != null ? hub.getAddress() : hub.getName());
            endPoint = hubPoint;

            if (startPoint != null || currentPoint != null) {
                handleStartNavigationClick();
            }
            updateMapMarkers();
            showToast("Đã chọn Hub: " + hub.getName() + " làm điểm đích.");
        }
    }

    protected double distanceMeters(@NonNull GeoPoint from, @NonNull GeoPoint to) {
        android.location.Location fromLocation = new android.location.Location("from");
        fromLocation.setLatitude(from.getLatitude());
        fromLocation.setLongitude(from.getLongitude());

        android.location.Location toLocation = new android.location.Location("to");
        toLocation.setLatitude(to.getLatitude());
        toLocation.setLongitude(to.getLongitude());

        return fromLocation.distanceTo(toLocation);
    }

    public void openDrawer() {
        hubSearchInteractionHelper.openDrawer(binding);
    }

    public void closeDrawer() {
        hubSearchInteractionHelper.closeDrawer(binding);
    }
}
