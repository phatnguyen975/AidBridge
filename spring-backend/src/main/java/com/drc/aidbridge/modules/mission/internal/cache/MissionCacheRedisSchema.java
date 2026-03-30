package com.drc.aidbridge.modules.mission.internal.cache;

import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionTrackingResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

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

    public void cacheMission(MissionResponse mission) {
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

    public Optional<MissionResponse> getCachedMission(UUID missionId) {
        try {
            String key = buildMissionKey(missionId);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.debug("Cache hit for mission {}", missionId);
                return Optional.of(objectMapper.readValue(json, MissionResponse.class));
            }
            log.debug("Cache miss for mission {}", missionId);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached mission {}", missionId, e);
        }
        return Optional.empty();
    }

    public void invalidateMissionCache(UUID missionId) {
        String key = buildMissionKey(missionId);
        Boolean deleted = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Mission {} cache invalidated", missionId);
        }
    }

    public void removeMissionFromCache(UUID missionId) {
        invalidateMissionCache(missionId);
        invalidateTrackingCache(missionId);
        log.info("Mission {} removed from all caches", missionId);
    }

    public void cacheTracking(MissionTrackingResponse tracking) {
        try {
            String key = buildTrackingKey(tracking.getMissionId());
            String json = objectMapper.writeValueAsString(tracking);
            redisTemplate.opsForValue().set(key, json, TRACKING_CACHE_TTL);
            log.debug("Tracking for mission {} cached", tracking.getMissionId());
        } catch (JsonProcessingException e) {
            log.error("Failed to cache tracking for mission {}", tracking.getMissionId(), e);
        }
    }

    public Optional<MissionTrackingResponse> getCachedTracking(UUID missionId) {
        try {
            String key = buildTrackingKey(missionId);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, MissionTrackingResponse.class));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached tracking for mission {}", missionId, e);
        }
        return Optional.empty();
    }

    public void invalidateTrackingCache(UUID missionId) {
        String key = buildTrackingKey(missionId);
        redisTemplate.delete(key);
    }

    private String buildMissionKey(UUID missionId) {
        return String.format("%s:%s", MISSION_PREFIX, missionId);
    }

    private String buildTrackingKey(UUID missionId) {
        return String.format("%s:%s", TRACKING_PREFIX, missionId);
    }

    private boolean isActiveStatus(String status) {
        return status.equals("ASSIGNED") ||
                status.equals("PICKING_UP") ||
                status.equals("PICKED_UP") ||
                status.equals("IN_TRANSIT") ||
                status.equals("DISPATCHING");
    }

    public boolean isMissionCached(UUID missionId) {
        String key = buildMissionKey(missionId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public Long getMissionCacheTTL(UUID missionId) {
        String key = buildMissionKey(missionId);
        return redisTemplate.getExpire(key);
    }
}
