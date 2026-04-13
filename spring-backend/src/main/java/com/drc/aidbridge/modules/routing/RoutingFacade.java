package com.drc.aidbridge.modules.routing;

/**
 * Public Facade for the Routing module.
 * Other modules (e.g. mission/) should ONLY import this interface + RoutingDTO.
 * Never import from routing.internal.*
 */
public interface RoutingFacade {

    /**
     * Calculate route between two coordinates.
     *
     * @return RoutingDTO with distance (m), duration (s), and encoded polyline
     */
    RoutingDTO calculateRoute(double startLat, double startLon,
                              double endLat, double endLon);

    /**
     * @return true if the GraphHopper engine is initialized and ready
     */
    boolean isReady();
}
