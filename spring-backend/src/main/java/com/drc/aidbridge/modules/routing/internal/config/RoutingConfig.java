package com.drc.aidbridge.modules.routing.internal.config;

import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperConfig;
import com.graphhopper.config.LMProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.jackson.Jackson;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.GHUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * GraphHopper initialization for the routing module.
 *
 * <p>Uses GraphHopper 11 typed profile API and a minimal custom model so bootRun works with
 * graphhopper-core 11.0.</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(GraphHopperProperties.class)
public class RoutingConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.routing.enabled", havingValue = "true", matchIfMissing = true)
    public GraphHopper graphHopper(GraphHopperProperties properties, ResourceLoader resourceLoader) throws Exception {
        Path osmPath = resolveOsmPath(properties, resourceLoader);

        log.info("OSM file ready: {} ({} MB)", osmPath.toAbsolutePath(), Files.size(osmPath) / (1024 * 1024));
        log.info("Initializing GraphHopper engine (GH 11.x)...");
        log.info("  OSM file    : {}", osmPath.toAbsolutePath());
        log.info("  Graph cache : {}", Path.of(properties.getGraphDir()).toAbsolutePath());
        log.info("  Data access : {}", properties.getDataAccess());

        GraphHopperConfig ghConfig = new GraphHopperConfig();
        ghConfig.putObject("datareader.file", osmPath.toString());
        ghConfig.putObject("graph.location", properties.getGraphDir());
        ghConfig.putObject("graph.dataaccess", properties.getDataAccess());
        ghConfig.putObject("import.osm.ignored_highways", "footway,cycleway,path,pedestrian,steps");
        ghConfig.putObject("graph.encoded_values", "car_access,car_average_speed,road_access,road_environment,max_speed,ferry_speed,road_class,roundabout,surface,smoothness");

        GraphHopper hopper = new GraphHopper();
        // Load all configured routing strategies (profiles) from properties
        List<Profile> profiles = new ArrayList<>();
        List<LMProfile> lmProfiles = new ArrayList<>();
        
        CustomModel defaultModel = GHUtility.loadCustomModelFromJar("car.json");
        log.info("Loaded default car model from JAR");
        
        for (Map.Entry<String, String> entry : properties.getProfiles().entrySet()) {
            String profileName = entry.getKey(); // e.g., "fastest", "safest"
            String customModelPath = entry.getValue(); // e.g., "routing-profiles/car-fastest.json"
            try {
                CustomModel customModel = loadCustomModelFromClasspath(customModelPath, resourceLoader);
                profiles.add(new Profile(profileName).setCustomModel(customModel));
                log.info("Loaded custom model for profile '{}' from {}", profileName, customModelPath);
            } catch (Exception ex) {
                // Fallback to default model if custom loading fails
                log.warn("Failed to load custom model for '{}' ({}), using default car model", profileName, ex.getMessage());
                profiles.add(new Profile(profileName).setCustomModel(defaultModel));
            }
            lmProfiles.add(new LMProfile(profileName));
        }

        // Fallback to basic car profile if no profiles configured
        if (profiles.isEmpty()) {
            log.warn("No routing profiles configured, using default car profile");
            profiles.add(new Profile("car").setCustomModel(defaultModel));
            lmProfiles.add(new LMProfile("car"));
        }

        log.info("Total profiles registered: {}", profiles.size());
        ghConfig.setProfiles(profiles);
        hopper.getLMPreparationHandler().setLMProfiles(lmProfiles);
        log.info("Building/loading graph... (first run can take 10-20 minutes)");
        
        try {
            hopper.init(ghConfig);
            long start = System.currentTimeMillis();
            hopper.importOrLoad();
            long elapsed = System.currentTimeMillis() - start;
            log.info("GraphHopper ready in {} seconds", elapsed / 1000);
            return hopper;
        } catch (Exception ex) {
            String msg = ex.getMessage() == null ? "" : ex.getMessage();
            boolean cacheIncompatible = msg.contains("Not a GraphHopper file")
                    || msg.contains("Profiles do not match");

            if (cacheIncompatible) {
                log.warn("Detected incompatible GraphHopper cache at {}. Rebuilding cache once.", properties.getGraphDir());
                try {
                    hopper.close();
                } catch (Exception ignored) {
                }

                deleteDirectory(Path.of(properties.getGraphDir()));

                GraphHopper retryHopper = new GraphHopper();
                retryHopper.init(ghConfig);
                long retryStart = System.currentTimeMillis();
                retryHopper.importOrLoad();
                long retryElapsed = System.currentTimeMillis() - retryStart;
                log.info("GraphHopper ready after cache rebuild in {} seconds", retryElapsed / 1000);
                return retryHopper;
            }

            log.error("GraphHopper initialization failed", ex);
            try {
                hopper.close();
            } catch (Exception ignored) {
            }
            throw ex;
        }
    }

    private CustomModel loadCustomModelFromClasspath(String resourcePath, ResourceLoader resourceLoader) throws Exception {
        // Load custom model JSON from classpath and deserialize to CustomModel object
        String resourceUrl = "classpath:" + resourcePath;
        Resource resource = resourceLoader.getResource(resourceUrl);
        if (!resource.exists()) {
            log.debug("Resource check for '{}': not found", resourceUrl);
            throw new IllegalArgumentException("Custom model file not found: " + resourceUrl);
        }
        try (InputStream is = resource.getInputStream()) {
            log.debug("Loading custom model from {}", resourcePath);
            var mapper = Jackson.newObjectMapper();
            CustomModel model = mapper.readValue(is, CustomModel.class);
            log.debug("Successfully deserialized custom model: {}", resourcePath);
            return model;
        } catch (Exception ex) {
            log.error("Deserialization error for {}: {} - {}", resourcePath, ex.getClass().getSimpleName(), ex.getMessage());
            throw new Exception("Failed to deserialize custom model '" + resourcePath + "': " + ex.getClass().getSimpleName() + " - " + ex.getMessage(), ex);
        }
    }

    private Path resolveOsmPath(GraphHopperProperties properties, ResourceLoader resourceLoader) throws Exception {
        Path configuredPath = Path.of(properties.getOsmFile());
        if (!configuredPath.isAbsolute()) {
            configuredPath = Path.of(System.getProperty("user.dir")).resolve(configuredPath).normalize();
        }
        if (Files.exists(configuredPath)) {
            return configuredPath;
        }

        Resource classpathResource = resourceLoader.getResource("classpath:vietnam.osm.pbf");
        if (!classpathResource.exists()) {
            throw new IllegalArgumentException("OSM file not found at configured path and classpath:vietnam.osm.pbf is missing");
        }

        try {
            return classpathResource.getFile().toPath();
        } catch (Exception ex) {
            Path dataDir = Path.of(System.getProperty("user.dir"), "data");
            Files.createDirectories(dataDir);
            Path extractedPath = dataDir.resolve("vietnam.osm.pbf");
            try (InputStream in = classpathResource.getInputStream()) {
                Files.copy(in, extractedPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return extractedPath;
        }
    }

    private void deleteDirectory(Path directory) throws Exception {
        if (!Files.exists(directory)) {
            return;
        }
        try (var walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception deleteEx) {
                    throw new RuntimeException(deleteEx);
                }
            });
        } catch (RuntimeException wrapped) {
            if (wrapped.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw wrapped;
        }
    }
}
