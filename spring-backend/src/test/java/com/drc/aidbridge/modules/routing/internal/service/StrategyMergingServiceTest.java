package com.drc.aidbridge.modules.routing.internal.service;

import com.drc.aidbridge.modules.routing.internal.web.dto.DangerousZone;
import com.drc.aidbridge.modules.routing.internal.web.dto.GeoJsonGeometry;
import com.graphhopper.json.Statement;
import com.graphhopper.util.CustomModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StrategyMergingServiceTest {

    private final StrategyMergingService service = new StrategyMergingService();

    @Test
    void applyDangerousZones_ShouldCreateDynamicAreaAndPriorityStatement() {
        CustomModel model = new CustomModel();
        DangerousZone zone = DangerousZone.builder()
                .name("flood-zone 1")
                .priority(0.25)
                .geometry(new GeoJsonGeometry("Polygon", List.of(List.of(
                        List.of(106.7000, 10.8000),
                        List.of(106.7100, 10.8000),
                        List.of(106.7100, 10.8100),
                        List.of(106.7000, 10.8100)
                ))))
                .build();

        service.applyDangerousZones(model, List.of(zone));

        assertEquals(1, model.getAreas().getFeatures().size());
        assertEquals("flood_zone_1", model.getAreas().getFeatures().get(0).getId());

        assertEquals(1, model.getPriority().size());
        Statement statement = model.getPriority().get(0);
        assertEquals(Statement.Keyword.IF, statement.keyword());
        assertEquals("in_flood_zone_1", statement.condition());
        assertEquals(Statement.Op.MULTIPLY, statement.operation());
        assertEquals("0.25", statement.value());
    }

    @Test
    void applyDangerousZones_ShouldUseFallbackAreaIdAndDefaultPriority() {
        CustomModel model = new CustomModel();
        DangerousZone zone = DangerousZone.builder()
                .name("###")
                .priority(null)
                .geometry(new GeoJsonGeometry("Polygon", List.of(List.of(
                        List.of(106.7000, 10.8000),
                        List.of(106.7100, 10.8000),
                        List.of(106.7100, 10.8100),
                        List.of(106.7000, 10.8100)
                ))))
                .build();

        service.applyDangerousZones(model, List.of(zone));

        assertEquals("zone_1", model.getAreas().getFeatures().get(0).getId());
        Statement statement = model.getPriority().get(0);
        assertEquals("in_zone_1", statement.condition());
        assertEquals("0.0", statement.value());
    }

    @Test
    void applyDangerousZones_ShouldThrowWhenGeometryTypeIsNotPolygon() {
        CustomModel model = new CustomModel();
        DangerousZone zone = DangerousZone.builder()
                .name("bad_zone")
                .priority(0.0)
                .geometry(new GeoJsonGeometry("Point", List.of(List.of(
                        List.of(106.7000, 10.8000)
                ))))
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.applyDangerousZones(model, List.of(zone)));

        assertEquals("Dangerous zone 'bad_zone' must use GeoJSON type Polygon", ex.getMessage());
    }
}