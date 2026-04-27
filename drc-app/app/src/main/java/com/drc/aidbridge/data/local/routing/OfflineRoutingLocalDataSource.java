package com.drc.aidbridge.data.local.routing;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drc.aidbridge.data.remote.dto.request.RoutingRequestDto;
import com.drc.aidbridge.data.remote.dto.response.RoutingResponseDto;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperConfig;
import com.graphhopper.config.Profile;
import com.graphhopper.jackson.Jackson;
import com.graphhopper.ResponsePath;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class OfflineRoutingLocalDataSource {

    private static final String TAG = "OfflineRouting";
    private static final String ASSET_GRAPH_DATA_DIR = "graph-data";
    private static final String ASSET_ROUTING_PROFILE_DIR = "routing-profiles";
    private static final String LOCAL_GRAPH_DATA_DIR = "graph-data";
    private static final String DEFAULT_PROFILE = "urgent_response";
    private static final String DEFAULT_GRAPH_DATA_ACCESS = "MMAP";
    private static final String DEFAULT_IGNORED_HIGHWAYS = "footway,cycleway,path,pedestrian,steps";
    private static final String DEFAULT_GRAPH_ENCODED_VALUES = "car_access,car_average_speed,road_access,road_environment,max_speed,"
            + "ferry_speed,road_class,roundabout,surface,smoothness";
    private static final String ENCODING_MISMATCH_MESSAGE = "Incompatible encoding version";
    private static final List<String> REQUIRED_GRAPH_FILES = Arrays.asList(
            "nodes",
            "edges",
            "properties",
            "geometry",
            "location_index",
            "edgekv_keys",
            "edgekv_vals");
    private static final List<String> SUPPORTED_STRATEGY_PROFILES = Arrays.asList(
            "urgent_response",
            "disaster_safe",
            "heavy_aid",
            "community_delivery",
            "offroad_terrain");

    private final Context appContext;
    private final Object graphHopperLock = new Object();

    @Nullable
    private GraphHopper graphHopper;

    @Inject
    public OfflineRoutingLocalDataSource(@ApplicationContext Context appContext) {
        this.appContext = appContext;
    }

    public boolean hasGraphData() {
        File localGraphDir = new File(appContext.getFilesDir(), LOCAL_GRAPH_DATA_DIR);
        boolean localGraphReady = containsGraphDataFiles(localGraphDir);
        boolean assetGraphReady = containsGraphDataAssets();
        boolean routingProfilesReady = hasRoutingProfileAssets();

        Log.d(
                TAG,
                "Offline data check -> localGraphReady=" + localGraphReady
                        + ", assetGraphReady=" + assetGraphReady
                        + ", routingProfilesReady=" + routingProfilesReady);

        return (localGraphReady || assetGraphReady) && routingProfilesReady;
    }

    @Nullable
    public RoutingResponseDto calculateRoute(@NonNull RoutingRequestDto requestDto) {
        GraphHopper localHopper = ensureGraphHopperLoaded();
        if (localHopper == null) {
            return null;
        }

        String preferredProfile = resolveProfile(requestDto.getStrategy());
        RoutingResponseDto preferredResult = calculateWithProfile(localHopper, requestDto, preferredProfile);
        if (preferredResult != null) {
            return preferredResult;
        }

        if (DEFAULT_PROFILE.equalsIgnoreCase(preferredProfile)) {
            return null;
        }

        return calculateWithProfile(localHopper, requestDto, DEFAULT_PROFILE);
    }

    @Nullable
    private GraphHopper ensureGraphHopperLoaded() {
        synchronized (graphHopperLock) {
            if (graphHopper != null) {
                return graphHopper;
            }

            try {
                File graphDir = ensureGraphDirectoryReady();
                if (graphDir == null) {
                    return null;
                }
                GraphHopper loadedHopper = tryLoadGraph(graphDir);
                if (loadedHopper != null) {
                    graphHopper = loadedHopper;
                    return graphHopper;
                }

                File refreshedGraphDir = refreshGraphDirectoryFromAssets();
                if (refreshedGraphDir == null) {
                    return null;
                }

                loadedHopper = tryLoadGraph(refreshedGraphDir);
                if (loadedHopper == null) {
                    return null;
                }

                graphHopper = loadedHopper;
                return graphHopper;
            } catch (Throwable t) {
                Log.e(TAG, "Failed to initialize offline GraphHopper", t);
                return null;
            }
        }
    }

    @Nullable
    private GraphHopper tryLoadGraph(@NonNull File graphDir) throws IOException {
        try {
            GraphHopper hopper = buildGraphHopper(graphDir);
            boolean loaded = hopper.load();
            if (!loaded) {
                Log.w(TAG, "GraphHopper failed to load graph-data at " + graphDir.getAbsolutePath());
                return null;
            }
            return hopper;
        } catch (IllegalStateException e) {
            if (!isEncodingVersionMismatch(e)) {
                throw e;
            }
            Log.w(TAG, "GraphHopper encoding mismatch detected, will refresh local graph cache", e);
            return null;
        }
    }

    @NonNull
    private GraphHopper buildGraphHopper(@NonNull File graphDir) throws IOException {
        GraphHopperConfig graphHopperConfig = new GraphHopperConfig();
        graphHopperConfig.putObject("graph.location", graphDir.getAbsolutePath());
        graphHopperConfig.putObject("graph.dataaccess", DEFAULT_GRAPH_DATA_ACCESS);
        graphHopperConfig.putObject("import.osm.ignored_highways", DEFAULT_IGNORED_HIGHWAYS);
        graphHopperConfig.putObject("graph.encoded_values", DEFAULT_GRAPH_ENCODED_VALUES);
        graphHopperConfig.setProfiles(loadProfilesFromAssets());

        GraphHopper hopper = new GraphHopper();
        hopper.init(graphHopperConfig);
        return hopper;
    }

    private boolean isEncodingVersionMismatch(@NonNull Throwable throwable) {
        String message = throwable.getMessage();
        return message != null && message.contains(ENCODING_MISMATCH_MESSAGE);
    }

    @Nullable
    private File ensureGraphDirectoryReady() throws IOException {
        File localGraphDir = new File(appContext.getFilesDir(), LOCAL_GRAPH_DATA_DIR);
        if (containsGraphDataFiles(localGraphDir)) {
            return localGraphDir;
        }

        if (!copyGraphDataFromAssets(localGraphDir)) {
            return null;
        }

        return containsGraphDataFiles(localGraphDir) ? localGraphDir : null;
    }

    @Nullable
    private File refreshGraphDirectoryFromAssets() throws IOException {
        File localGraphDir = new File(appContext.getFilesDir(), LOCAL_GRAPH_DATA_DIR);
        if (localGraphDir.exists() && !deleteRecursively(localGraphDir)) {
            Log.w(TAG, "Unable to clear local graph cache at " + localGraphDir.getAbsolutePath());
            return null;
        }

        if (!copyGraphDataFromAssets(localGraphDir)) {
            return null;
        }

        return containsGraphDataFiles(localGraphDir) ? localGraphDir : null;
    }

    private boolean containsGraphDataFiles(@Nullable File graphDir) {
        if (graphDir == null || !graphDir.exists() || !graphDir.isDirectory()) {
            return false;
        }

        for (String requiredFile : REQUIRED_GRAPH_FILES) {
            if (!new File(graphDir, requiredFile).exists()) {
                return false;
            }
        }
        return true;
    }

    private boolean containsGraphDataAssets() {
        try {
            String[] entries = appContext.getAssets().list(ASSET_GRAPH_DATA_DIR);
            if (entries == null || entries.length == 0) {
                Log.w(TAG, "graph-data asset directory is missing or empty");
                return false;
            }

            List<String> availableFiles = Arrays.asList(entries);
            for (String requiredFile : REQUIRED_GRAPH_FILES) {
                if (!availableFiles.contains(requiredFile)) {
                    Log.w(TAG, "Missing graph-data asset file: " + requiredFile);
                    return false;
                }
            }

            return true;
        } catch (IOException e) {
            Log.w(TAG, "Unable to inspect graph-data assets", e);
            return false;
        }
    }

    private boolean copyGraphDataFromAssets(@NonNull File targetDir) throws IOException {
        AssetManager assetManager = appContext.getAssets();
        String[] rootEntries = assetManager.list(ASSET_GRAPH_DATA_DIR);
        if (rootEntries == null || rootEntries.length == 0) {
            Log.w(TAG, "graph-data assets are missing");
            return false;
        }

        copyAssetNode(assetManager, ASSET_GRAPH_DATA_DIR, targetDir);
        return true;
    }

    private void copyAssetNode(@NonNull AssetManager assetManager,
            @NonNull String assetPath,
            @NonNull File target) throws IOException {
        String[] children = assetManager.list(assetPath);
        if (children == null || children.length == 0) {
            copyAssetFile(assetManager, assetPath, target);
            return;
        }

        if (!target.exists() && !target.mkdirs()) {
            throw new IOException("Unable to create directory: " + target.getAbsolutePath());
        }

        for (String child : children) {
            String childAssetPath = assetPath + "/" + child;
            File childTarget = new File(target, child);
            copyAssetNode(assetManager, childAssetPath, childTarget);
        }
    }

    private void copyAssetFile(@NonNull AssetManager assetManager,
            @NonNull String assetPath,
            @NonNull File targetFile) throws IOException {
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create parent directory: " + parent.getAbsolutePath());
        }

        try (InputStream inputStream = assetManager.open(assetPath);
                OutputStream outputStream = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
        }
    }

    private boolean deleteRecursively(@NonNull File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    @Nullable
    private RoutingResponseDto calculateWithProfile(@NonNull GraphHopper localHopper,
            @NonNull RoutingRequestDto requestDto,
            @NonNull String profile) {
        GHRequest ghRequest = new GHRequest(
                requestDto.getStartLat(),
                requestDto.getStartLon(),
                requestDto.getEndLat(),
                requestDto.getEndLon());
        ghRequest.setProfile(profile);
        ghRequest.setLocale(Locale.forLanguageTag("vi"));

        GHResponse ghResponse = localHopper.route(ghRequest);
        if (ghResponse.hasErrors()) {
            Log.w(TAG, "Offline routing failed for profile " + profile + ": "
                    + ghResponse.getErrors().get(0).getMessage());
            return null;
        }

        ResponsePath path = ghResponse.getBest();
        if (path == null || path.getPoints() == null || path.getPoints().size() == 0) {
            return null;
        }

        return new RoutingResponseDto(
                path.getDistance(),
                path.getTime() / 1000L,
                encodePolyline(path.getPoints()),
                System.currentTimeMillis(),
                mapInstructions(path.getInstructions()),
                RoutingResponseDto.ROUTE_SOURCE_OFFLINE);
    }

    @NonNull
    private String resolveProfile(@Nullable String strategy) {
        if (strategy == null || strategy.trim().isEmpty()) {
            return DEFAULT_PROFILE;
        }
        return strategy.trim();
    }

    @NonNull
    private List<RoutingResponseDto.InstructionDto> mapInstructions(@Nullable InstructionList instructionList) {
        if (instructionList == null || instructionList.isEmpty()) {
            return Collections.emptyList();
        }

        List<RoutingResponseDto.InstructionDto> mappedInstructions = new ArrayList<>();
        for (Instruction instruction : instructionList) {
            int turnType = instruction.getSign();
            mappedInstructions.add(new RoutingResponseDto.InstructionDto(
                    turnType,
                    instruction.getName(),
                    instruction.getDistance(),
                    instruction.getTime(),
                    resolveTurnCommand(turnType)));
        }

        return mappedInstructions;
    }

    @NonNull
    private String resolveTurnCommand(int turnType) {
        switch (turnType) {
            case -98:
                return "U-turn (khong xac dinh)";
            case -8:
                return "U-turn trai";
            case -7:
                return "Giu trai";
            case -6:
                return "Roi khoi vong xoay";
            case -3:
                return "Re gap trai";
            case -2:
                return "Re trai";
            case -1:
                return "Re nhe trai";
            case 0:
                return "Tiep tuc";
            case 1:
                return "Re nhe phai";
            case 2:
                return "Re phai";
            case 3:
                return "Re gap phai";
            case 4:
                return "Ket thuc";
            case 5:
                return "Truoc diem via";
            case 6:
                return "Truoc khi vao vong xoay";
            case 7:
                return "Giu phai";
            case 8:
                return "U-turn phai";
            default:
                return "Khong xac dinh";
        }
    }

    @NonNull
    private String encodePolyline(@NonNull PointList points) {
        StringBuilder encoded = new StringBuilder();
        int previousLat = 0;
        int previousLon = 0;

        for (int i = 0; i < points.size(); i++) {
            int lat = (int) Math.round(points.getLat(i) * 1e5);
            int lon = (int) Math.round(points.getLon(i) * 1e5);

            encodeSignedNumber(lat - previousLat, encoded);
            encodeSignedNumber(lon - previousLon, encoded);

            previousLat = lat;
            previousLon = lon;
        }

        return encoded.toString();
    }

    @NonNull
    private List<Profile> loadProfilesFromAssets() throws IOException {
        List<Profile> profiles = new ArrayList<>();
        for (String profileName : SUPPORTED_STRATEGY_PROFILES) {
            String assetPath = ASSET_ROUTING_PROFILE_DIR + "/car-" + profileName + ".json";
            profiles.add(new Profile(profileName).setCustomModel(readCustomModel(assetPath)));
        }
        return profiles;
    }

    @NonNull
    private CustomModel readCustomModel(@NonNull String assetPath) throws IOException {
        try (InputStream inputStream = appContext.getAssets().open(assetPath)) {
            return Jackson.newObjectMapper().readValue(inputStream, CustomModel.class);
        }
    }

    private boolean hasRoutingProfileAssets() {
        AssetManager assetManager = appContext.getAssets();
        for (String profileName : SUPPORTED_STRATEGY_PROFILES) {
            String assetPath = ASSET_ROUTING_PROFILE_DIR + "/car-" + profileName + ".json";
            try (InputStream ignored = assetManager.open(assetPath)) {
                // Asset exists and can be opened.
            } catch (IOException e) {
                Log.w(TAG, "Missing routing profile asset: " + assetPath, e);
                return false;
            }
        }
        return true;
    }

    private void encodeSignedNumber(int value, @NonNull StringBuilder output) {
        int shifted = value << 1;
        if (value < 0) {
            shifted = ~shifted;
        }

        while (shifted >= 0x20) {
            output.append((char) ((0x20 | (shifted & 0x1F)) + 63));
            shifted >>= 5;
        }
        output.append((char) (shifted + 63));
    }
}
