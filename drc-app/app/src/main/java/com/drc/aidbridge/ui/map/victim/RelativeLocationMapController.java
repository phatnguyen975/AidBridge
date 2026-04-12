package com.drc.aidbridge.ui.map.victim;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Controls a minimal victim-relative map that only displays one pinned location.
 */
public class RelativeLocationMapController implements OnMapReadyCallback {

    private static final String MAP_TAG = "VictimRelativeLocationMap";
    private static final float DEFAULT_ZOOM = 16f;

    private final Fragment hostFragment;
    private final int mapContainerId;

    private GoogleMap googleMap;
    private Marker pinnedMarker;
    private LatLng pendingPin;
    private String pendingTitle;

    public RelativeLocationMapController(@NonNull Fragment hostFragment, int mapContainerId) {
        this.hostFragment = hostFragment;
        this.mapContainerId = mapContainerId;
    }

    public void initialize() {
        if (!hostFragment.isAdded()) {
            return;
        }

        FragmentManager fragmentManager = hostFragment.getChildFragmentManager();
        SupportMapFragment mapFragment = findOrCreateMapFragment(fragmentManager);
        mapFragment.getMapAsync(this);
    }

    public void pinLocation(double latitude, double longitude, @Nullable String markerTitle) {
        LatLng target = new LatLng(latitude, longitude);
        pendingPin = target;
        pendingTitle = markerTitle != null ? markerTitle.trim() : "";

        if (googleMap == null) {
            return;
        }

        applyPin(target, pendingTitle);
    }

    public void clearPin() {
        pendingPin = null;
        pendingTitle = "";
        if (pinnedMarker != null) {
            pinnedMarker.remove();
            pinnedMarker = null;
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap readyMap) {
        googleMap = readyMap;
        configureMapUi(googleMap);

        if (pendingPin != null) {
            applyPin(pendingPin, pendingTitle);
        }
    }

    private SupportMapFragment findOrCreateMapFragment(FragmentManager fragmentManager) {
        Fragment byId = fragmentManager.findFragmentById(mapContainerId);
        if (byId instanceof SupportMapFragment) {
            return (SupportMapFragment) byId;
        }

        Fragment byTag = fragmentManager.findFragmentByTag(MAP_TAG);
        if (byTag instanceof SupportMapFragment) {
            fragmentManager.beginTransaction()
                .replace(mapContainerId, byTag, MAP_TAG)
                .commitNowAllowingStateLoss();
            return (SupportMapFragment) byTag;
        }

        SupportMapFragment newMapFragment = SupportMapFragment.newInstance();
        fragmentManager.beginTransaction()
            .replace(mapContainerId, newMapFragment, MAP_TAG)
            .commitNowAllowingStateLoss();
        return newMapFragment;
    }

    private void configureMapUi(GoogleMap map) {
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);

        // Keep map interactive for manual inspection after searching an address.
        map.getUiSettings().setScrollGesturesEnabled(true);
        map.getUiSettings().setZoomGesturesEnabled(true);
        map.getUiSettings().setRotateGesturesEnabled(true);
        map.getUiSettings().setTiltGesturesEnabled(true);
    }

    private void applyPin(LatLng target, String markerTitle) {
        if (googleMap == null) {
            return;
        }

        if (pinnedMarker != null) {
            pinnedMarker.remove();
        }

        String safeTitle = markerTitle != null && !markerTitle.trim().isEmpty()
            ? markerTitle.trim()
            : null;

        pinnedMarker = googleMap.addMarker(
            new MarkerOptions()
                .position(target)
                .title(safeTitle)
        );
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, DEFAULT_ZOOM));
    }
}
