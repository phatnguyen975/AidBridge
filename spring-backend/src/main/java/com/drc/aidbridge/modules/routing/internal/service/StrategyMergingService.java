package com.drc.aidbridge.modules.routing.internal.service;

import com.drc.aidbridge.modules.routing.internal.web.dto.DangerousZone;
import com.drc.aidbridge.modules.routing.internal.web.dto.GeoJsonGeometry;
import com.graphhopper.json.Statement;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.JsonFeature;
import com.graphhopper.util.JsonFeatureCollection;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;

/**
 * Service for merging multiple routing strategies and applying dangerous zones to GraphHopper custom models.
 */
@Slf4j
@Service
public class StrategyMergingService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    /**
     * Merge multiple CustomModels by combining priority and speed rules, preferring more restrictive speed limits.
     */
    public CustomModel mergeStrategies(List<CustomModel> models) {
        if (models == null || models.isEmpty()) {
            return new CustomModel();
        }
        if (models.size() == 1) {
            return models.get(0);
        }

        // Start with first strategy and progressively merge others
        CustomModel merged = new CustomModel(models.get(0));
        for (int i = 1; i < models.size(); i++) {
            mergeIntoTarget(merged, models.get(i));
        }
        return merged;
    }

    /**
     * Merge source strategy into target by adding its priority and speed rules (more restrictive wins).
     */
    private void mergeIntoTarget(CustomModel target, CustomModel source) {
        if (source == null) return;

        // Add source priority rules to target (higher priority multiplier = stronger preference)
        List<Statement> sourcePriority = source.getPriority();
        if (sourcePriority != null) {
            sourcePriority.forEach(stmt -> target.addToPriority(stmt));
        }

        // Add source speed rules to target (lower speed limit = more restrictive, safer)
        List<Statement> sourceSpeed = source.getSpeed();
        if (sourceSpeed != null) {
            sourceSpeed.forEach(stmt -> target.addToSpeed(stmt));
        }

        if (source.getAreas() != null) {
            target.addAreas(source.getAreas());
        }
    }

    /**
     * Apply dangerous zones to custom model as GeoJSON area constraints with priority multipliers.
     */
    public void applyDangerousZones(CustomModel model, List<DangerousZone> zones) {
        if (model == null || zones == null || zones.isEmpty()) {
            return;
        }

        JsonFeatureCollection areas = new JsonFeatureCollection();
        int fallbackIndex = 1;

        for (DangerousZone zone : zones) {
            if (zone == null || zone.getGeometry() == null) {
                continue;
            }

            double priority = zone.getPriority() != null ? zone.getPriority() : 0.0; // Default: bypass
            String areaId = buildAreaId(zone.getName(), fallbackIndex++);

            JsonFeature feature = new JsonFeature();
            feature.setId(areaId);
            feature.setGeometry(toPolygon(zone.getGeometry(), zone.getName()));
            areas.getFeatures().add(feature);

            model.addToPriority(Statement.If("in_" + areaId, Statement.Op.MULTIPLY, String.valueOf(priority)));
            log.debug("Applied dangerous zone '{}' as area '{}' with priority multiplier {}", zone.getName(), areaId, priority);
        }

        if (!areas.getFeatures().isEmpty()) {
            model.addAreas(areas);
        }
    }

    private String buildAreaId(String zoneName, int fallbackIndex) {
        String normalized = zoneName == null ? "" : zoneName.toLowerCase(Locale.ROOT).trim();
        normalized = normalized.replaceAll("[^a-z0-9_]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");

        if (normalized.isEmpty() || Character.isDigit(normalized.charAt(0))) {
            normalized = "zone_" + fallbackIndex;
        }

        return normalized;
    }

    private Polygon toPolygon(GeoJsonGeometry geometry, String zoneName) {
        if (geometry.getType() == null || !"Polygon".equalsIgnoreCase(geometry.getType())) {
            throw new IllegalArgumentException("Dangerous zone '" + zoneName + "' must use GeoJSON type Polygon");
        }

        List<List<List<Double>>> rings = geometry.getCoordinates();
        if (rings == null || rings.isEmpty()) {
            throw new IllegalArgumentException("Dangerous zone '" + zoneName + "' must contain polygon coordinates");
        }

        LinearRing shell = toLinearRing(rings.get(0), zoneName);
        LinearRing[] holes = new LinearRing[Math.max(0, rings.size() - 1)];
        for (int i = 1; i < rings.size(); i++) {
            holes[i - 1] = toLinearRing(rings.get(i), zoneName);
        }

        return GEOMETRY_FACTORY.createPolygon(shell, holes);
    }

    private LinearRing toLinearRing(List<List<Double>> ringPoints, String zoneName) {
        if (ringPoints == null || ringPoints.size() < 3) {
            throw new IllegalArgumentException("Dangerous zone '" + zoneName + "' must contain at least 3 coordinates");
        }

        List<Coordinate> coordinates = new ArrayList<>();
        for (List<Double> point : ringPoints) {
            if (point == null || point.size() < 2 || point.get(0) == null || point.get(1) == null) {
                throw new IllegalArgumentException("Dangerous zone '" + zoneName + "' contains invalid coordinate point");
            }
            coordinates.add(new Coordinate(point.get(0), point.get(1)));
        }

        Coordinate first = coordinates.get(0);
        Coordinate last = coordinates.get(coordinates.size() - 1);
        if (!first.equals2D(last)) {
            coordinates.add(new Coordinate(first.x, first.y));
        }

        if (coordinates.size() < 4) {
            throw new IllegalArgumentException("Dangerous zone '" + zoneName + "' polygon ring must contain at least 4 points");
        }

        return GEOMETRY_FACTORY.createLinearRing(coordinates.toArray(new Coordinate[0]));
    }
}
