package com.drc.aidbridge.modules.volunteer.internal.test;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;

@Slf4j
public class H3Visualizer {

    public static void generateMapHtml(H3Core h3Core, String centerHex, List<String> kRingList, double centerLat, double centerLng, List<Volunteer> volunteers) {
        try {
            StringBuilder js = new StringBuilder();

            // Draw center hex (blue)
            List<LatLng> centerBoundary = h3Core.cellToBoundary(centerHex);
            js.append("L.polygon([");
            for (LatLng pt : centerBoundary) {
                js.append("[").append(pt.lat).append(",").append(pt.lng).append("],");
            }
            js.append("], {color: 'blue', fillColor: '#03f', fillOpacity: 0.5}).addTo(map).bindPopup('Center: ").append(centerHex).append("');\n");

            // Draw kRing hexes (red)
            for (String hex : kRingList) {
                if (hex.equals(centerHex)) continue;
                List<LatLng> boundary = h3Core.cellToBoundary(hex);
                js.append("L.polygon([");
                for (LatLng pt : boundary) {
                    js.append("[").append(pt.lat).append(",").append(pt.lng).append("],");
                }
                js.append("], {color: 'red', fillColor: '#f03', fillOpacity: 0.2}).addTo(map).bindPopup('Hex: ").append(hex).append("');\n");
            }

            if (volunteers != null) {
                for (Volunteer v : volunteers) {
                    if (v.getCurrentLocation() != null) {
                        js.append("L.circleMarker([")
                          .append(v.getCurrentLocation().getY()).append(", ")
                          .append(v.getCurrentLocation().getX()).append("], {radius: 8, color: 'green', fillColor: '#3f0', fillOpacity: 0.8}).addTo(map)")
                          .append(".bindPopup('Volunteer ID: ").append(v.getId()).append("');\n");
                    }
                }
            }

            String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <title>H3 Spatial Visualizer</title>\n" +
                    "    <meta charset=\"utf-8\" />\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
                    "    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
                    "    <style>\n" +
                    "        html, body { height: 100%; margin: 0; }\n" +
                    "        #map { height: 100%; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<div id=\"map\"></div>\n" +
                    "<script>\n" +
                    "    var map = L.map('map').setView([" + centerLat + ", " + centerLng + "], 14);\n" +
                    "    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
                    "        attribution: '&copy; OpenStreetMap contributors'\n" +
                    "    }).addTo(map);\n" +
                    "    L.marker([" + centerLat + ", " + centerLng + "]).addTo(map)\n" +
                    "        .bindPopup('Victim Center')\n" +
                    "        .openPopup();\n" +
                    "    " + js.toString() + "\n" +
                    "</script>\n" +
                    "</body>\n" +
                    "</html>";

            Path dir = Paths.get("d:\\HCMUS\\Third Year\\Ultra Android Mobile\\AidBridge\\spring-backend\\src\\main\\java\\com\\drc\\aidbridge\\modules\\volunteer\\internal\\test");
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path file = dir.resolve("map.html");
            try (FileWriter writer = new FileWriter(file.toFile())) {
                writer.write(html);
            }
            log.info("H3 visualizer HTML saved successfully to: {}", file.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to generate H3 visualization HTML: {}", e.getMessage());
        }
    }
}
