package com.drc.aidbridge.ui.map.victim;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drc.aidbridge.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

/**
 * Controls a minimal victim-relative map that only displays one pinned location
 * using the app's osmdroid/GraphHopper map stack.
 */
public class RelativeLocationMapController {

    private static final float DEFAULT_ZOOM = 16f;
    private static final double DEFAULT_CENTER_LAT = 10.7769;
    private static final double DEFAULT_CENTER_LON = 106.7009;

    private final Fragment hostFragment;
    private final int mapContainerId;

    private MapView mapView;
    private Marker pinnedMarker;
    private GeoPoint pendingPin;
    private String pendingTitle;

    public RelativeLocationMapController(@NonNull Fragment hostFragment, int mapContainerId) {
        this.hostFragment = hostFragment;
        this.mapContainerId = mapContainerId;
    }

    public void initialize() {
        if (!hostFragment.isAdded()) {
            return;
        }
        mapView = hostFragment.requireView().findViewById(mapContainerId);
        if (mapView == null) {
            return;
        }

        Context context = hostFragment.requireContext().getApplicationContext();
        Configuration.getInstance().setUserAgentValue(context.getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);

        MapController controller = (MapController) mapView.getController();
        controller.setZoom(DEFAULT_ZOOM);
        controller.setCenter(new GeoPoint(DEFAULT_CENTER_LAT, DEFAULT_CENTER_LON));

        if (pendingPin != null) {
            applyPin(pendingPin, pendingTitle);
        }
    }

    public void pinLocation(double latitude, double longitude, @Nullable String markerTitle) {
        GeoPoint target = new GeoPoint(latitude, longitude);
        pendingPin = target;
        pendingTitle = markerTitle != null ? markerTitle.trim() : "";

        if (mapView == null) {
            return;
        }

        applyPin(target, pendingTitle);
    }

    public void clearPin() {
        pendingPin = null;
        pendingTitle = "";
        if (pinnedMarker != null) {
            mapView.getOverlays().remove(pinnedMarker);
            pinnedMarker = null;
            mapView.invalidate();
        }
    }

    public void onResume() {
        if (mapView != null) {
            mapView.onResume();
        }
    }

    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
    }

    public void onDestroy() {
        if (mapView != null) {
            mapView.getOverlays().clear();
            mapView.onDetach();
            mapView = null;
        }
        pinnedMarker = null;
    }

    private void applyPin(GeoPoint target, String markerTitle) {
        if (mapView == null) {
            return;
        }

        if (pinnedMarker != null) {
            mapView.getOverlays().remove(pinnedMarker);
        }

        String safeTitle = markerTitle != null && !markerTitle.trim().isEmpty()
            ? markerTitle.trim()
            : null;

        pinnedMarker = new Marker(mapView);
        pinnedMarker.setPosition(target);
        pinnedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        pinnedMarker.setIcon(androidx.core.content.ContextCompat.getDrawable(
                hostFragment.requireContext(),
                R.drawable.bg_map_end_marker
        ));
        if (safeTitle != null) {
            pinnedMarker.setTitle(safeTitle);
            pinnedMarker.showInfoWindow();
        }

        mapView.getOverlays().add(pinnedMarker);
        mapView.getController().setZoom(DEFAULT_ZOOM);
        mapView.getController().animateTo(target);
        mapView.invalidate();
    }
}
