package com.drc.aidbridge.modules.routing.internal.util;

import com.drc.aidbridge.modules.routing.internal.web.dto.DangerousZone;
import com.graphhopper.util.PointList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Utility to write an HTML viewer (Leaflet) for GraphHopper route with optional dangerous zones overlay.
 * Writes file to project's test folder for quick developer visualization.
 */
public final class RouteViewer {
    private static final Logger log = LoggerFactory.getLogger(RouteViewer.class);

    private RouteViewer() {}

    public static Path writeRouteHtml(PointList points) throws IOException {
        return writeRouteHtml(points, Double.NaN, Double.NaN, Double.NaN, Double.NaN, List.of());
    }

    /**
     * Write HTML viewer showing route, requested points, and dangerous zones (if any).
     */
    public static Path writeRouteHtml(PointList points, double reqStartLat, double reqStartLon, 
                                      double reqEndLat, double reqEndLon) throws IOException {
        return writeRouteHtml(points, reqStartLat, reqStartLon, reqEndLat, reqEndLon, List.of());
    }

    /**
     * Write HTML viewer with route, requested points, and dangerous zones overlay.
     */
    public static Path writeRouteHtml(PointList points, double reqStartLat, double reqStartLon,
                                      double reqEndLat, double reqEndLon, List<DangerousZone> dangerousZones) throws IOException {
        // Build JavaScript array of route points
        StringBuilder ptsJs = new StringBuilder();
        ptsJs.append("[");
        for (int i = 0; i < points.size(); i++) {
            ptsJs.append("[").append(points.getLat(i)).append(",").append(points.getLon(i)).append("]");
            if (i + 1 < points.size()) ptsJs.append(',');
        }
        ptsJs.append("]");

        // Build dangerous zones JavaScript array
        StringBuilder zonesJs = new StringBuilder();
        zonesJs.append("[");
        if (dangerousZones != null && !dangerousZones.isEmpty()) {
            for (int i = 0; i < dangerousZones.size(); i++) {
                DangerousZone zone = dangerousZones.get(i);
                zonesJs.append("{name:'").append(zone.getName()).append("',");
                zonesJs.append("priority:").append(zone.getPriority() != null ? zone.getPriority() : 0.0).append(",");
                zonesJs.append("coords:[");
                
                // Convert GeoJSON coordinates to leaflet format
                if (zone.getGeometry() != null && zone.getGeometry().getCoordinates() != null) {
                    List<List<List<Double>>> rings = zone.getGeometry().getCoordinates();
                    if (!rings.isEmpty()) {
                        List<List<Double>> ring = rings.get(0); // Outer ring
                        for (int j = 0; j < ring.size(); j++) {
                            List<Double> coord = ring.get(j);
                            if (coord.size() >= 2) {
                                zonesJs.append("[").append(coord.get(1)).append(",").append(coord.get(0)).append("]"); // [lat, lng]
                                if (j + 1 < ring.size()) zonesJs.append(',');
                            }
                        }
                    }
                }
                zonesJs.append("]}");
                if (i + 1 < dangerousZones.size()) zonesJs.append(',');
            }
        }
        zonesJs.append("]");

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>\n");
        html.append("<html><head><meta charset='utf-8'/><title>Route preview with dangerous zones</title>\n");
        html.append("<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.3/dist/leaflet.css'/>\n");
        html.append("<style>html,body,#map{height:100%;margin:0;padding:0} ");
        html.append(".info{background:rgba(255,255,255,0.9);padding:6px;border-radius:4px;font-family:Arial,Helvetica,sans-serif;font-size:12px} ");
        html.append(".legend{background:rgba(255,255,255,0.9);padding:8px;border-radius:4px;font-family:Arial,sans-serif;font-size:11px} ");
        html.append(".legend-item{margin:4px 0} .legend-color{display:inline-block;width:16px;height:16px;margin-right:4px;border-radius:2px}");
        html.append("</style>\n");
        html.append("</head><body>\n");
        html.append("<div id='map'></div>\n");
        html.append("<script src='https://unpkg.com/leaflet@1.9.3/dist/leaflet.js'></script>\n");
        html.append("<script>\n");
        html.append("  const pts = ").append(ptsJs.toString()).append(";\n");
        html.append("  const zones = ").append(zonesJs.toString()).append(";\n");
        html.append("  var allPts = pts.slice();\n");
        html.append("  function addIfValid(lat,lon){ if(!isNaN(lat) && !isNaN(lon)) allPts.push([lat,lon]); }\n");
        html.append("  addIfValid(").append(reqStartLat).append(",").append(reqStartLon).append(");\n");
        html.append("  addIfValid(").append(reqEndLat).append(",").append(reqEndLon).append(");\n");
        html.append("  const map = L.map('map');\n");
        html.append("  if (allPts.length > 0) { map.fitBounds(allPts, {padding:[20,20]}); } else { map.setView([0,0],2); }\n");
        html.append("  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19}).addTo(map);\n");
        
        // Draw dangerous zones as colored polygons
        html.append("  zones.forEach((zone, idx) => {\n");
        html.append("    const color = zone.priority === 0 ? '#FF0000' : '#FFA500';\n"); // Red for bypass (0), Orange for reduce
        html.append("    const polygon = L.polygon(zone.coords, {\n");
        html.append("      color: color, weight: 2, opacity: 0.8, fillOpacity: 0.3, fillColor: color\n");
        html.append("    }).addTo(map);\n");
        html.append("    polygon.bindPopup(`<strong>\uD83D\uDEA8 ${zone.name}</strong><br/>Priority: ${zone.priority}`).openPopup();\n");
        html.append("  });\n");
        
        // Draw route polyline
        html.append("  if (pts.length > 0) {\n");
        html.append("    const poly = L.polyline(pts, {color:'deepskyblue', weight:5, opacity:0.95}).addTo(map);\n");
        html.append("    const routeStart = pts[0]; const routeEnd = pts[pts.length-1];\n");
        html.append("    L.circleMarker(routeStart, {radius:7, color:'white', weight:2, fillColor:'green', fillOpacity:1}).addTo(map).bindPopup('Snapped start');\n");
        html.append("    L.circleMarker(routeEnd, {radius:7, color:'white', weight:2, fillColor:'red', fillOpacity:1}).addTo(map).bindPopup('Snapped end');\n");
        html.append("  }\n");
        
        // Draw requested start/end points
        html.append("  if(!isNaN(").append(reqStartLat).append(") && !isNaN(").append(reqStartLon).append(")) {\n");
        html.append("    L.circleMarker([").append(reqStartLat).append(',').append(reqStartLon).append("], {radius:6, color:'#000', weight:1, fillColor:'blue', fillOpacity:0.9}).addTo(map).bindPopup('Requested start');\n");
        html.append("  }\n");
        html.append("  if(!isNaN(").append(reqEndLat).append(") && !isNaN(").append(reqEndLon).append(")) {\n");
        html.append("    L.circleMarker([").append(reqEndLat).append(',').append(reqEndLon).append("], {radius:6, color:'#000', weight:1, fillColor:'orange', fillOpacity:0.9}).addTo(map).bindPopup('Requested end');\n");
        html.append("  }\n");
        
        // Add info control with route statistics and legend
        html.append("  const info = L.control({position:'topright'});\n");
        html.append("  info.onAdd = function() {\n");
        html.append("    const div = L.DomUtil.create('div','info');\n");
        html.append("    div.innerHTML = '<strong>Route Preview</strong><br/>Points: ' + pts.length + '<br/>Zones: ' + zones.length;\n");
        html.append("    return div;\n");
        html.append("  };\n");
        html.append("  info.addTo(map);\n");
        
        // Add legend for colors
        html.append("  const legend = L.control({position:'bottomright'});\n");
        html.append("  legend.onAdd = function() {\n");
        html.append("    const div = L.DomUtil.create('div','legend');\n");
        html.append("    div.innerHTML = '<strong>Legend</strong><br/>';\n");
        html.append("    div.innerHTML += '<div class=\"legend-item\"><span class=\"legend-color\" style=\"background:#FF0000\"></span>Bypass (priority 0)</div>';\n");
        html.append("    div.innerHTML += '<div class=\"legend-item\"><span class=\"legend-color\" style=\"background:#FFA500\"></span>Reduce (priority > 0)</div>';\n");
        html.append("    div.innerHTML += '<div class=\"legend-item\"><span class=\"legend-color\" style=\"background:deepskyblue\"></span>Route path</div>';\n");
        html.append("    return div;\n");
        html.append("  };\n");
        html.append("  legend.addTo(map);\n");
        html.append("</script>\n");
        html.append("</body></html>");

        // Resolve output path
        Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path moduleBase;
        if (cwd.getFileName() != null && cwd.getFileName().toString().equals("spring-backend")) {
            moduleBase = cwd;
        } else {
            moduleBase = cwd.resolve("spring-backend");
        }

        Path out = moduleBase.resolve(Paths.get("src","main","java","com","drc","aidbridge","modules","routing","internal","test","route.html"));
        Files.createDirectories(out.getParent());
        Files.writeString(out, html.toString());
        log.info("Wrote route viewer: {} (zones: {})", out.toAbsolutePath(), dangerousZones != null ? dangerousZones.size() : 0);
        return out;
    }
}

