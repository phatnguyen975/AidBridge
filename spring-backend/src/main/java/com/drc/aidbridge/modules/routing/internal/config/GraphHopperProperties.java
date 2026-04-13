package com.drc.aidbridge.modules.routing.internal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Binds to the {@code app.routing.*} keys in application.yaml for GraphHopper configuration.
 */
@Data
@ConfigurationProperties(prefix = "app.routing")
public class GraphHopperProperties {

    /** Path to the .osm.pbf map file (relative to working directory or absolute). */
    private String osmFile = "data/vietnam.osm.pbf";

    /** Directory where the built graph is cached for fast reloads. */
    private String graphDir = "./graph-data";

    /** DataAccess type: MMAP (low RAM) or RAM_STORE (fast, high RAM). */
    private String dataAccess = "MMAP";

    /** Enable/disable routing module initialization. */
    private boolean enabled = true;

    /** Map of routing strategy profiles: strategy_name -> profile_file_path. */
    private Map<String, String> profiles = new LinkedHashMap<>();
}
