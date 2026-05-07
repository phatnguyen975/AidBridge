package com.drc.aidbridge.ui.map.base.helper;

import android.content.Context;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
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
    private final List<Polyline> routeOverlays = new ArrayList<>();
    private final List<Polyline> historyOverlays = new ArrayList<>();
    private final List<Polygon> dangerousZoneOverlays = new ArrayList<>();

    public void attach(@NonNull MapView mapView) {
        this.mapView = mapView;
    }

    public void detach() {
        clearRouteOverlays();
        clearHistoryOverlays();
        clearDangerousZoneOverlays();
        this.mapView = null;
    }

    public void clearRouteOverlays() {
        if (mapView == null) {
            routeOverlays.clear();
            return;
        }

        for (Polyline polyline : routeOverlays) {
            mapView.getOverlays().remove(polyline);
        }
        routeOverlays.clear();
    }

    public void clearHistoryOverlays() {
        if (mapView == null) {
            historyOverlays.clear();
            return;
        }

        for (Polyline polyline : historyOverlays) {
            mapView.getOverlays().remove(polyline);
        }
        historyOverlays.clear();
    }

    public void drawRouteOverlays(@NonNull Context context,
                                  @NonNull List<GeoPoint> points,
                                  @ColorRes int casingColorRes,
                                  @DimenRes int casingWidthRes,
                                  @ColorRes int coreColorRes,
                                  @DimenRes int coreWidthRes,
                                  @DimenRes int fitPaddingRes,
                                  boolean shouldZoom) {
        if (mapView == null || points.isEmpty()) {
            return;
        }

        clearRouteOverlays();

        float baseCasingWidth = context.getResources().getDimension(casingWidthRes);
        int casingColor = ContextCompat.getColor(context, casingColorRes);
        int coreColor = ContextCompat.getColor(context, coreColorRes);

        // 1. Outer Glow
        Polyline outerGlow = new Polyline();
        outerGlow.setPoints(points);
        outerGlow.setColor((casingColor & 0x00FFFFFF) | 0x20000000);
        outerGlow.setWidth(baseCasingWidth * 1.5f);
        configurePolylinePaint(outerGlow.getOutlinePaint());
        mapView.getOverlays().add(outerGlow);
        routeOverlays.add(outerGlow);

        // 2. Inner Glow
        Polyline innerGlow = new Polyline();
        innerGlow.setPoints(points);
        innerGlow.setColor((casingColor & 0x00FFFFFF) | 0x50000000);
        innerGlow.setWidth(baseCasingWidth * 1.f);
        configurePolylinePaint(innerGlow.getOutlinePaint());
        mapView.getOverlays().add(innerGlow);
        routeOverlays.add(innerGlow);

        // 3. Casing
        Polyline casing = new Polyline();
        casing.setPoints(points);
        casing.setColor(casingColor);
        casing.setWidth(baseCasingWidth);
        configurePolylinePaint(casing.getOutlinePaint());
        mapView.getOverlays().add(casing);
        routeOverlays.add(casing);

        // 4. Core
        Polyline core = new Polyline();
        core.setPoints(points);
        core.setColor(coreColor);
        core.setWidth(context.getResources().getDimension(coreWidthRes));
        configurePolylinePaint(core.getOutlinePaint());
        mapView.getOverlays().add(core);
        routeOverlays.add(core);

        if (shouldZoom) {
            BoundingBox bounds = BoundingBox.fromGeoPointsSafe(points);
            mapView.zoomToBoundingBox(bounds, true, context.getResources().getDimensionPixelSize(fitPaddingRes));
        }
        mapView.invalidate();
    }

    public void drawHistoryOverlays(@NonNull Context context,
                                    @NonNull List<GeoPoint> points,
                                    @ColorRes int casingColorRes,
                                    @DimenRes int casingWidthRes,
                                    @ColorRes int coreColorRes,
                                    @DimenRes int coreWidthRes) {
        if (mapView == null || points.size() < 2) {
            return;
        }

        clearHistoryOverlays();

        float baseCasingWidth = context.getResources().getDimension(casingWidthRes);
        int casingColor = ContextCompat.getColor(context, casingColorRes);
        int coreColor = ContextCompat.getColor(context, coreColorRes);

        // History Casing
        Polyline casing = new Polyline();
        casing.setPoints(points);
        casing.setColor(casingColor);
        casing.setWidth(baseCasingWidth);
        configurePolylinePaint(casing.getOutlinePaint());
        mapView.getOverlays().add(casing);
        historyOverlays.add(casing);

        // History Core
        Polyline core = new Polyline();
        core.setPoints(points);
        core.setColor(coreColor);
        core.setWidth(context.getResources().getDimension(coreWidthRes));
        configurePolylinePaint(core.getOutlinePaint());
        mapView.getOverlays().add(core);
        historyOverlays.add(core);

        mapView.invalidate();
    }

    private void configurePolylinePaint(@NonNull Paint paint) {
        paint.setAntiAlias(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setPathEffect(new CornerPathEffect(30f));
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
