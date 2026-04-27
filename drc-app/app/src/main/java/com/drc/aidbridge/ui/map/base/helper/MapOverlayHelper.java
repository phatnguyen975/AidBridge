package com.drc.aidbridge.ui.map.base.helper;

import android.content.Context;
import android.location.Location;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.drc.aidbridge.data.remote.dto.request.RoutingRequestDto;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class MapOverlayHelper {

    @Nullable
    private MapView mapView;
    @Nullable
    private Polyline routePolylineCasing;
    @Nullable
    private Polyline routePolyline;
    private final List<Polygon> dangerousZoneOverlays = new ArrayList<>();

    public void attach(@NonNull MapView mapView) {
        this.mapView = mapView;
    }

    public void detach() {
        clearRouteOverlays();
        clearDangerousZoneOverlays();
        this.mapView = null;
    }

    public void clearRouteOverlays() {
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
    }

    public void drawRouteOverlays(@NonNull Context context,
                                  @NonNull List<GeoPoint> points,
                                  @ColorRes int casingColorRes,
                                  @DimenRes int casingWidthRes,
                                  @ColorRes int coreColorRes,
                                  @DimenRes int coreWidthRes,
                                  @DimenRes int fitPaddingRes) {
        if (mapView == null || points.isEmpty()) {
            return;
        }

        clearRouteOverlays();

        routePolylineCasing = new Polyline();
        routePolylineCasing.setPoints(points);
        routePolylineCasing.setColor(ContextCompat.getColor(context, casingColorRes));
        routePolylineCasing.setWidth(context.getResources().getDimension(casingWidthRes));
        routePolylineCasing.getOutlinePaint().setAntiAlias(true);
        mapView.getOverlays().add(routePolylineCasing);

        routePolyline = new Polyline();
        routePolyline.setPoints(points);
        routePolyline.setColor(ContextCompat.getColor(context, coreColorRes));
        routePolyline.setWidth(context.getResources().getDimension(coreWidthRes));
        routePolyline.getOutlinePaint().setAntiAlias(true);
        mapView.getOverlays().add(routePolyline);

        BoundingBox bounds = BoundingBox.fromGeoPointsSafe(points);
        mapView.zoomToBoundingBox(bounds, true, context.getResources().getDimensionPixelSize(fitPaddingRes));
        mapView.invalidate();
    }

    public void renderDangerousZoneOverlays(@NonNull Context context,
                                           @NonNull List<RoutingRequestDto.DangerousZoneDto> zones,
                                           @ColorRes int fillColorRes,
                                           @ColorRes int strokeColorRes,
                                           @DimenRes int strokeWidthRes) {
        if (mapView == null) {
            return;
        }

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
            polygon.setFillColor(ContextCompat.getColor(context, fillColorRes));
            polygon.setStrokeColor(ContextCompat.getColor(context, strokeColorRes));
            polygon.setStrokeWidth(context.getResources().getDimension(strokeWidthRes));
            mapView.getOverlays().add(polygon);
            dangerousZoneOverlays.add(polygon);
        }

        mapView.invalidate();
    }

    public void clearDangerousZoneOverlays() {
        if (mapView == null) {
            dangerousZoneOverlays.clear();
            return;
        }

        for (Polygon polygon : dangerousZoneOverlays) {
            mapView.getOverlays().remove(polygon);
        }
        dangerousZoneOverlays.clear();
    }

    @NonNull
    public List<GeoPoint> decodePolyline(@NonNull String encodedPolyline) {
        List<GeoPoint> points = new ArrayList<>();
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

                points.add(new GeoPoint(latitude / 1E5d, longitude / 1E5d));
            }
        } catch (Exception ignored) {
            points.clear();
        }

        return points;
    }

    @NonNull
    public List<GeoPoint> smoothRoutePoints(@NonNull List<GeoPoint> originalPoints,
                                            double smoothingSegmentMeters,
                                            int smoothingMaxSteps) {
        if (originalPoints.size() < 2) {
            return originalPoints;
        }

        List<GeoPoint> smoothed = new ArrayList<>();
        smoothed.add(originalPoints.get(0));

        for (int i = 1; i < originalPoints.size(); i++) {
            GeoPoint from = originalPoints.get(i - 1);
            GeoPoint to = originalPoints.get(i);

            double segmentDistance = distanceMeters(from, to);
            int steps = (int) Math.ceil(segmentDistance / smoothingSegmentMeters);
            steps = Math.max(1, Math.min(steps, smoothingMaxSteps));

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
