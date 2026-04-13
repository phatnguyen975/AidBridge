package com.drc.aidbridge.modules.routing.internal.usecase;

import com.drc.aidbridge.modules.routing.RoutingDTO;
import com.drc.aidbridge.modules.routing.internal.service.StrategyMergingService;
import com.drc.aidbridge.modules.routing.internal.util.PolylineEncoder;
import com.drc.aidbridge.modules.routing.internal.web.dto.RoutingRequest;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.drc.aidbridge.modules.routing.internal.util.RouteViewer;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Use case for calculating driving routes with strategy selection and dangerous zone avoidance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CalculateRouteUseCase {

    private final GraphHopper graphHopper;
    private final StrategyMergingService strategyMergingService;

    public RoutingDTO execute(RoutingRequest request) {

        // 1. Determine strategy (use primary, fallback to "urgent_response" if missing)
        String strategy = request.getStrategy() != null && !request.getStrategy().isBlank()
                ? request.getStrategy()
                : "urgent_response";  // default strategy if none provided

        // 2. Build GHRequest with primary strategy
        GHRequest ghRequest = new GHRequest(
                request.getStartLat(), request.getStartLon(),
                request.getEndLat(), request.getEndLon()
        );
        ghRequest.setProfile(strategy);
        ghRequest.setLocale(Locale.forLanguageTag("vi"));

        // 3. Apply request-time options (dangerous zones)
        boolean hasDangerousZones = request.getDangerousZones() != null && !request.getDangerousZones().isEmpty();

        if (hasDangerousZones) {
            // Determine penalty intensity based on strategy type
            String penaltyIntensity = determineDangerPenaltyIntensity(strategy);
            
            CustomModel requestCustomModel = new CustomModel();
            strategyMergingService.applyDangerousZones(requestCustomModel, request.getDangerousZones());
            ghRequest.setCustomModel(requestCustomModel);
            log.info("Applied {} dynamic dangerous zones (intensity={}) for strategy '{}'",
                    request.getDangerousZones().size(), penaltyIntensity, strategy);
        }

        // 4. Execute routing
        long start = System.currentTimeMillis();
        GHResponse response = graphHopper.route(ghRequest);
        long elapsed = System.currentTimeMillis() - start;

        log.info("Routing request: start=({}, {}), end=({}, {}), strategy={}, dangerousZones={}, elapsed={}ms",
                request.getStartLat(), request.getStartLon(),
                request.getEndLat(), request.getEndLon(),
                strategy,
                hasDangerousZones ? request.getDangerousZones().size() : 0,
                elapsed);

        // 5. Handle errors
        if (response.hasErrors()) {
            String errorMsg = response.getErrors().get(0).getMessage();
            log.warn("Routing failed: {} ({}ms)", errorMsg, elapsed);
            throw new IllegalStateException("Routing failed: " + errorMsg);
        }

        // 6. Extract best path
        ResponsePath path = response.getBest();
        PointList points = path.getPoints();

        // Quick debug: log input vs snapped points and write HTML viewer
        try {
            if (points.size() > 0) {
                log.info("Route start: ({}, {}), end: ({}, {})",
                        points.getLat(0), points.getLon(0),
                        points.getLat(points.size() - 1), points.getLon(points.size() - 1));
            }
            Path viewer = RouteViewer.writeRouteHtml(points,
                    request.getStartLat(), request.getStartLon(),
                    request.getEndLat(), request.getEndLon(),
                    request.getDangerousZones());
            log.info("Route viewer written: {}", viewer.toAbsolutePath());
        } catch (Exception e) {
            log.warn("Failed to write route viewer", e);
        }

        // 7. Encode to Google Maps polyline (precision 5)
        String polyline = PolylineEncoder.encode(points);

        // 8. Extract turn-by-turn instructions
        List<RoutingDTO.Instruction> instructions = extractInstructions(path.getInstructions());

        log.debug("Route result: distance={}m, duration={}s, points={}, instructions={}, time={}ms",
                Math.round(path.getDistance()),
                path.getTime() / 1000,
                points.size(),
                instructions.size(),
                elapsed);

        return new RoutingDTO(
                path.getDistance(),
                path.getTime() / 1000,   // ms → seconds
                polyline,
                System.currentTimeMillis(),
                instructions
        );
    }

    /**
     * Determine danger zone penalty intensity based on routing strategy.
     * 
     * @param strategy The primary routing strategy
     * @return "HARD" for safety-focused strategies, "SOFT" for offroad/terrain strategies
     */
    private String determineDangerPenaltyIntensity(String strategy) {
        // Offroad strategy: reduce penalty intensity (danger zones less critical when traversing difficult terrain)
        if ("offroad_terrain".equalsIgnoreCase(strategy)) {
            return "SOFT";  // penalize ~0.7 instead of ~0.1
        }
        
        // All other strategies: hard penalty for danger zones
        // urgent_response: still avoid danger despite speed priority
        // disaster_safe: maximum avoidance
        // heavy_aid: maximum avoidance
        // community_delivery: maximum avoidance
        return "HARD";  // aggressive penalty ~0.1
    }

    private List<RoutingDTO.Instruction> extractInstructions(InstructionList instructionList) {
        List<RoutingDTO.Instruction> result = new ArrayList<>();
        
        if (instructionList == null || instructionList.isEmpty()) {
            return result;
        }

        // Convert GraphHopper instructions to DTO format
        for (com.graphhopper.util.Instruction gh : instructionList) {
            int turnType = gh.getSign();
            RoutingDTO.Instruction instruction = new RoutingDTO.Instruction(
                    turnType,                       // turn type (-98 to 8)
                    gh.getName() != null ? gh.getName() : "",  // street name
                    gh.getDistance(),               // distance in meters
                    gh.getTime(),                   // time in milliseconds
                    RoutingDTO.Instruction.getTurnCommand(turnType)  // Vietnamese command
            );
            result.add(instruction);
        }

        return result;
    }
}
