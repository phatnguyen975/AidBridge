package com.drc.aidbridge.modules.hub.internal.usecase;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ListHubNearLocationUseCase {

    private final HubRepository hubRepository;
    private final HubMapper hubMapper;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    public List<HubDTO> execute(HubStatus status, double lat, double lon, double radius) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
        String statusName = (status != null) ? status.name() : null;
        return hubRepository.findByStatusAndLocationWithinRadius(statusName, point, radius)
                .stream()
                .map(hubMapper::toDTO)
                .toList();
    }
}
