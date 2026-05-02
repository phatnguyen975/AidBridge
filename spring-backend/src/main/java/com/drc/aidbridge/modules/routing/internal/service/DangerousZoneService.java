package com.drc.aidbridge.modules.routing.internal.service;

import com.drc.aidbridge.modules.routing.internal.entity.DangerousZone;
import com.drc.aidbridge.modules.routing.internal.repository.DangerousZoneRepository;
import com.drc.aidbridge.modules.routing.internal.web.dto.GeoJsonGeometry;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DangerousZoneService {

    private final DangerousZoneRepository repository;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    public List<DangerousZone> getAllZones() {
        return repository.findAll();
    }

    @Transactional
    public DangerousZone createZone(String name, GeoJsonGeometry geometry, UUID adminId) {
        Polygon polygon = convertToJtsPolygon(geometry);
        DangerousZone zone = DangerousZone.builder()
                .name(name)
                .area(polygon)
                .adminId(adminId)
                .build();
        return repository.save(zone);
    }

    @Transactional
    public DangerousZone updateZone(UUID id, String name, GeoJsonGeometry geometry) {
        DangerousZone zone = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dangerous zone not found with id: " + id));
        
        if (name != null) zone.setName(name);
        if (geometry != null) {
            zone.setArea(convertToJtsPolygon(geometry));
        }
        
        return repository.save(zone);
    }

    @Transactional
    public void deleteZone(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Dangerous zone not found with id: " + id);
        }
        repository.deleteById(id);
    }

    private Polygon convertToJtsPolygon(GeoJsonGeometry geometry) {
        if (geometry == null || !"Polygon".equalsIgnoreCase(geometry.getType())) {
            throw new IllegalArgumentException("Only Polygon geometry is supported");
        }

        List<List<List<Double>>> coords = geometry.getCoordinates();
        if (coords == null || coords.isEmpty()) {
            throw new IllegalArgumentException("Coordinates cannot be empty");
        }

        // Handle first ring (outer boundary)
        List<List<Double>> ringCoords = coords.get(0);
        Coordinate[] jtsCoords = new Coordinate[ringCoords.size()];
        for (int i = 0; i < ringCoords.size(); i++) {
            List<Double> point = ringCoords.get(i);
            jtsCoords[i] = new Coordinate(point.get(0), point.get(1)); // [lng, lat]
        }

        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(jtsCoords);
        Polygon polygon = GEOMETRY_FACTORY.createPolygon(shell, null);
        polygon.setSRID(4326);
        return polygon;
    }

    public com.drc.aidbridge.modules.routing.internal.web.dto.DangerousZoneResponse toDto(DangerousZone entity) {
        if (entity == null) return null;
        
        return com.drc.aidbridge.modules.routing.internal.web.dto.DangerousZoneResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .adminId(entity.getAdminId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .geometry(fromJtsPolygon(entity.getArea()))
                .build();
    }

    private GeoJsonGeometry fromJtsPolygon(Polygon polygon) {
        if (polygon == null) return null;
        
        List<List<List<Double>>> coordinates = new ArrayList<>();
        List<List<Double>> outerRing = new ArrayList<>();
        
        Coordinate[] jtsCoords = polygon.getExteriorRing().getCoordinates();
        for (Coordinate coord : jtsCoords) {
            outerRing.add(List.of(coord.x, coord.y));
        }
        
        coordinates.add(outerRing);
        return new GeoJsonGeometry("Polygon", coordinates);
    }
}
