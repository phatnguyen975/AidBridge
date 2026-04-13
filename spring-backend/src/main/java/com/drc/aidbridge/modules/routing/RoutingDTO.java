package com.drc.aidbridge.modules.routing;

import java.util.List;


/**
 * Public DTO for routing results.
 * Used by other modules (e.g. mission/) via RoutingFacade.
 *
 * @param distance      route distance in meters
 * @param duration      route duration in seconds
 * @param polyline      Google Maps encoded polyline (precision 5)
 * @param timestamp     server timestamp in milliseconds
 * @param instructions  turn-by-turn directions from start to end
 */
public record RoutingDTO(
        double distance,
        long duration,
        String polyline,
        long timestamp,
        List<Instruction> instructions
) {
    /**
     * Single turn-by-turn navigation instruction.
     *
     * @param turnType  instruction type (0=start, 1=turn_right, 2=turn_left, etc.)
     * @param name      street/road name
     * @param distance  distance to travel in meters
     * @param time      time in milliseconds
     */
    public record Instruction(
            int turnType,
            String name,
            double distance,
            long time
    ) {
    }
}
