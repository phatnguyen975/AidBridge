package com.drc.aidbridge.redis;

import com.drc.aidbridge.dto.response.MissionResponseDto;
import com.drc.aidbridge.dto.response.MissionTrackingResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis cache schema for Mission data.
 *
 * Key patterns:
 * - mission:{id} - Active mission cache (TTL: 5 minutes)
 * - mission:tracking:{id} - Mission tracking data (TTL: 30 seconds)
 * - volunteer:locations - Geo data for volunteer positions (managed by
 * DispatchRedisSchema)
 *
 * Cache Strategy:
 * - Only cache active missions (ASSIGNED, PICKING_UP, PICKED_UP, IN_TRANSIT)
 * - Invalidate on status change
 * - Remove from cache when mission completes or cancels
 */
@Slf4j
@Service
public class MissionCacheRedisSchema {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String MISSION_PREFIX = "mission";
    private static final String TRACKING_PREFIX = "mission:tracking";

    private static final Duration MISSION_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration TRACKING_CACHE_TTL = Duration.ofSeconds(30);

    public MissionCacheRedisSchema(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // ==================== Mission Cache Operations ====================

    /**
     * Cache an active mission.
     * Only caches missions with active status (ASSIGNED, PICKING_UP, PICKED_UP,
     * IN_TRANSIT).
     */
    public void cacheMission(MissionResponseDto mission) {
        if (!isActiveStatus(mission.getStatus().name())) {
            log.debug("Mission {} not cached - status {} is not active", mission.getId(), mission.getStatus());
            return;
        }

        try {
            String key = buildMissionKey(mission.getId());
            String json = objectMapper.writeValueAsString(mission);
            redisTemplate.opsForValue().set(key, json, MISSION_CACHE_TTL);
            log.debug("Mission {} cached successfully", mission.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to cache mission {}", mission.getId(), e);
        }
    }

    /**
     * Get cached mission by ID.
     */
    public Optional<MissionResponseDto> getCachedMission(UUID missionId) {
        try {
            String key = buildMissionKey(missionId);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.debug("Cache hit for mission {}", missionId);
                return Optional.of(objectMapper.readValue(json, MissionResponseDto.class));
            }
            log.debug("Cache miss for mission {}", missionId);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached mission {}", missionId, e);
        }
        return Optional.empty();
    }

    /**
     * Invalidate mission cache.
     * Call this when mission status changes.
     */
    public void invalidateMissionCache(UUID missionId) {
        String key = buildMissionKey(missionId);
        Boolean deleted = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Mission {} cache invalidated", missionId);
        }
    }

    /**
     * Remove mission from cache.
     * Call this when mission is completed or cancelled.
     */
    public void removeMissionFromCache(UUID missionId) {
        invalidateMissionCache(missionId);
        invalidateTrackingCache(missionId);
        log.info("Mission {} removed from all caches", missionId);
    }

    // ==================== Tracking Cache Operations ====================

    /**
     * Cache mission tracking data.
     * Short TTL since location updates frequently.
     */
    public void cacheTracking(MissionTrackingResponseDto tracking) {
        try {
            String key = buildTrackingKey(tracking.getMissionId());
            String json = objectMapper.writeValueAsString(tracking);
            redisTemplate.opsForValue().set(key, json, TRACKING_CACHE_TTL);
            log.debug("Tracking for mission {} cached", tracking.getMissionId());
        } catch (JsonProcessingException e) {
            log.error("Failed to cache tracking for mission {}", tracking.getMissionId(), e);
        }
    }

    /**
     * Get cached tracking data.
     */
    public Optional<MissionTrackingResponseDto> getCachedTracking(UUID missionId) {
        try {
            String key = buildTrackingKey(missionId);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, MissionTrackingResponseDto.class));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached tracking for mission {}", missionId, e);
        }
        return Optional.empty();
    }

    /**
     * Invalidate tracking cache.
     */
    public void invalidateTrackingCache(UUID missionId) {
        String key = buildTrackingKey(missionId);
        redisTemplate.delete(key);
    }

    // ==================== Helper Methods ====================

    private String buildMissionKey(UUID missionId) {
        return String.format("%s:%s", MISSION_PREFIX, missionId.toString());
    }

    private String buildTrackingKey(UUID missionId) {
        return String.format("%s:%s", TRACKING_PREFIX, missionId.toString());
    }

    /**
     * Check if status is considered "active" for caching purposes.
     */
    private boolean isActiveStatus(String status) {
        return status.equals("ASSIGNED") ||
                status.equals("PICKING_UP") ||
                status.equals("PICKED_UP") ||
                status.equals("IN_TRANSIT") ||
                status.equals("DISPATCHING");
    }

    /**
     * Check if mission exists in cache.
     */
    public boolean isMissionCached(UUID missionId) {
        String key = buildMissionKey(missionId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Get remaining TTL for cached mission (in seconds).
     */
    public Long getMissionCacheTTL(UUID missionId) {
        String key = buildMissionKey(missionId);
        return redisTemplate.getExpire(key);
    }
}
