package com.drc.aidbridge.ui.map.base.helper;

import android.content.Context;
import android.location.Location;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.dto.response.hub.HubDto;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class MapMarkerHelper {

    @Nullable
    private MapView mapView;

    @Nullable
    private Marker startMarker;
    @Nullable
    private Marker endMarker;
    @Nullable
    private Marker currentMarker;
    private final List<Marker> hubMarkers = new ArrayList<>();

    @Nullable
    private GeoPoint lastStartMarkerPoint;
    @Nullable
    private GeoPoint lastEndMarkerPoint;
    @Nullable
    private GeoPoint lastCurrentMarkerPoint;

    @Nullable
    private android.view.View currentPulseView;
    @Nullable
    private android.animation.ValueAnimator pulseAnimator;

    public boolean hasStartMarker() {
        return startMarker != null;
    }

    public boolean hasEndMarker() {
        return endMarker != null;
    }

    public boolean hasCurrentMarker() {
        return currentMarker != null;
    }

    public interface OnHubSelectedListener {
        void onHubSelected(HubDto hub);
    }

    public void attach(@NonNull MapView mapView) {
        this.mapView = mapView;
    }

    public void detach() {
        clearTrackedMarkers();
        clearHubMarkers();
        this.mapView = null;
    }

    public void updateStandardMarkers(@NonNull Context context,
                                      @Nullable GeoPoint startPoint,
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
        if (mapView == null) {
            return;
        }

        try {
            startMarker = renderMarker(context, startMarker, startPoint, lastStartMarkerPoint, startTitle, startIcon, startUpdateThresholdMeters);
            if (startPoint == null) {
                lastStartMarkerPoint = null;
            } else {
                lastStartMarkerPoint = startPoint;
            }

            endMarker = renderMarker(context, endMarker, endPoint, lastEndMarkerPoint, endTitle, endIcon, endUpdateThresholdMeters);
            if (endPoint == null) {
                lastEndMarkerPoint = null;
            } else {
                lastEndMarkerPoint = endPoint;
            }

            if (dynamicPoint != null) {
                if (currentPulseView == null) {
                    currentPulseView = android.view.LayoutInflater.from(context)
                            .inflate(R.layout.view_pulse_location, mapView, false);
                    
                    android.view.View locationPin = currentPulseView.findViewById(R.id.imgLocationPin);
                    if (locationPin != null) {
                        locationPin.post(() -> {
                            locationPin.setPivotX(locationPin.getWidth() / 2f);
                            locationPin.setPivotY(locationPin.getHeight());
                        });
                    }
                    pulseAnimator = android.animation.ValueAnimator.ofFloat(0.8f, 1.1f);
                    pulseAnimator.setDuration(1000);
                    pulseAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                    pulseAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                    pulseAnimator.addUpdateListener(animation -> {
                        if (locationPin != null) {
                            float animatedValue = (float) animation.getAnimatedValue();
                            locationPin.setScaleX(animatedValue);
                            locationPin.setScaleY(animatedValue);
                        }
                    });
                    pulseAnimator.start();

                    org.osmdroid.views.MapView.LayoutParams params = new org.osmdroid.views.MapView.LayoutParams(
                            org.osmdroid.views.MapView.LayoutParams.WRAP_CONTENT,
                            org.osmdroid.views.MapView.LayoutParams.WRAP_CONTENT,
                            dynamicPoint,
                            org.osmdroid.views.MapView.LayoutParams.BOTTOM_CENTER,
                            0, 0
                    );
                    mapView.addView(currentPulseView, params);
                } else {
                    org.osmdroid.views.MapView.LayoutParams params = 
                            (org.osmdroid.views.MapView.LayoutParams) currentPulseView.getLayoutParams();
                    params.geoPoint = dynamicPoint;
                    currentPulseView.setLayoutParams(params);
                }
            } else {
                if (currentPulseView != null) {
                    if (pulseAnimator != null) {
                        pulseAnimator.cancel();
                        pulseAnimator = null;
                    }
                    mapView.removeView(currentPulseView);
                    currentPulseView = null;
                }
            }
            
            if (dynamicPoint == null) {
                lastCurrentMarkerPoint = null;
            } else {
                lastCurrentMarkerPoint = dynamicPoint;
            }

            showTrackedMarkerPopups();
            mapView.invalidate();
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private Marker renderMarker(@NonNull Context context,
                                @Nullable Marker existing,
                                @Nullable GeoPoint nextPoint,
                                @Nullable GeoPoint previousPoint,
                                @NonNull String title,
                                @DrawableRes int iconRes,
                                double updateThresholdMeters) {
        if (mapView == null) {
            return existing;
        }

        if (nextPoint == null) {
            if (existing != null) {
                mapView.getOverlays().remove(existing);
            }
            return null;
        }

        boolean shouldRefresh = existing == null
                || previousPoint == null
                || distanceMeters(nextPoint, previousPoint) > updateThresholdMeters;
        if (!shouldRefresh) {
            return existing;
        }

        if (existing != null) {
            mapView.getOverlays().remove(existing);
        }

        Marker marker = new Marker(mapView);
        marker.setPosition(nextPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(context, iconRes));
        configureMarkerPopup(marker, title);
        mapView.getOverlays().add(marker);
        return marker;
    }

    private void configureMarkerPopup(@NonNull Marker marker, @NonNull String title) {
        marker.setTitle(title);
        marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);
        marker.setOnMarkerClickListener((clickedMarker, clickedMapView) -> {
            clickedMarker.showInfoWindow();
            return true;
        });
    }

    public void showTrackedMarkerPopups() {

        if (endMarker != null) {
            endMarker.showInfoWindow();
        }

    }

    public void clearTrackedMarkers() {
        if (mapView == null) {
            return;
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
        if (currentPulseView != null) {
            if (pulseAnimator != null) {
                pulseAnimator.cancel();
                pulseAnimator = null;
            }
            mapView.removeView(currentPulseView);
            currentPulseView = null;
        }

        lastStartMarkerPoint = null;
        lastEndMarkerPoint = null;
        lastCurrentMarkerPoint = null;
    }

    public void clearHubMarkers() {
        if (mapView != null) {
            for (Marker marker : hubMarkers) {
                mapView.getOverlays().remove(marker);
            }
        }
        hubMarkers.clear();
        if (mapView != null) {
            mapView.invalidate();
        }
    }

    public void drawHubMarkers(@NonNull Context context, @NonNull List<HubDto> hubs, @NonNull OnHubSelectedListener listener) {
        clearHubMarkers();
        if (mapView == null)
            return;
        for (HubDto hub : hubs) {
            if (hub.getLocation() != null) {
                Marker m = new Marker(mapView);
                m.setPosition(new GeoPoint(hub.getLocation().getLat(), hub.getLocation().getLng()));
                m.setTitle(hub.getName());
                m.setSnippet(hub.getAddress());
                m.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_hub_location_pin));
                m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                m.setOnMarkerClickListener((marker, mp) -> {
                    marker.showInfoWindow();
                    listener.onHubSelected(hub);
                    return true;
                });
                hubMarkers.add(m);
                mapView.getOverlays().add(m);
            }
        }
        mapView.invalidate();
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
}
