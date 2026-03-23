package com.drc.aidbridge.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis schema for Dispatch State Management.
 *
 * Key patterns:
 * - dispatch:task:{taskId} - Task dispatch state
 * - dispatch:volunteer:{volunteerId} - Volunteer availability
 * - dispatch:pending:{taskId} - Pending task assignments (30-second window)
 * - dispatch:queue:{priority} - Task queue by priority
 *
 * Use cases:
 * - Real-time task dispatch to volunteers
 * - 30-second acceptance window tracking
 * - Volunteer availability tracking
 * - Priority-based task queuing
 */
@Slf4j
@Service
public class DispatchRedisSchema {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String TASK_PREFIX = "dispatch:task";
    private static final String VOLUNTEER_PREFIX = "dispatch:volunteer";
    private static final String PENDING_PREFIX = "dispatch:pending";
    private static final String QUEUE_PREFIX = "dispatch:queue";
    private static final String ACTIVE_TASKS_KEY = "dispatch:active";

    private static final Duration PENDING_TTL = Duration.ofSeconds(30);
    private static final Duration TASK_TTL = Duration.ofHours(24);
    private static final Duration VOLUNTEER_TTL = Duration.ofMinutes(30);

    public DispatchRedisSchema(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public enum TaskStatus {
        PENDING, // Waiting for volunteer acceptance
        ACCEPTED, // Volunteer accepted
        IN_PROGRESS, // Task in progress
        COMPLETED, // Task completed
        CANCELLED, // Task cancelled
        EXPIRED // No volunteer accepted in time
    }

    public enum VolunteerStatus {
        AVAILABLE,
        BUSY,
        OFFLINE
    }

    public enum TaskPriority {
        CRITICAL(0),
        HIGH(1),
        MEDIUM(2),
        LOW(3);

        private final int value;

        TaskPriority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispatchTask implements Serializable {
        private Long taskId;
        private String taskType;
        private TaskStatus status;
        private TaskPriority priority;
        private Long assignedVolunteerId;
        private List<Long> notifiedVolunteerIds;
        private Double latitude;
        private Double longitude;
        private String description;
        private Long createdAt;
        private Long acceptedAt;
        private Long expiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolunteerState implements Serializable {
        private Long volunteerId;
        private VolunteerStatus status;
        private Long currentTaskId;
        private Double latitude;
        private Double longitude;
        private Long lastHeartbeat;
        private List<String> skills;
    }

    // ==================== Task Operations ====================

    /**
     * Create a new dispatch task and add to queue.
     */
    public void createTask(DispatchTask task) {
        try {
            task.setCreatedAt(Instant.now().toEpochMilli());
            task.setStatus(TaskStatus.PENDING);
            task.setExpiresAt(Instant.now().plusMillis(PENDING_TTL.toMillis()).toEpochMilli());

            String key = buildTaskKey(task.getTaskId());
            String json = objectMapper.writeValueAsString(task);
            redisTemplate.opsForValue().set(key, json, TASK_TTL);

            // Add to priority queue
            String queueKey = buildQueueKey(task.getPriority());
            redisTemplate.opsForZSet().add(queueKey, String.valueOf(task.getTaskId()), task.getCreatedAt());

            // Track active task
            redisTemplate.opsForSet().add(ACTIVE_TASKS_KEY, String.valueOf(task.getTaskId()));

            log.info("Dispatch task {} created with priority {}", task.getTaskId(), task.getPriority());
        } catch (JsonProcessingException e) {
            log.error("Failed to create dispatch task {}", task.getTaskId(), e);
        }
    }

    /**
     * Get task by ID.
     */
    public Optional<DispatchTask> getTask(Long taskId) {
        try {
            String key = buildTaskKey(taskId);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, DispatchTask.class));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to get dispatch task {}", taskId, e);
        }
        return Optional.empty();
    }

    /**
     * Update task status.
     */
    public void updateTaskStatus(Long taskId, TaskStatus status, Long volunteerId) {
        getTask(taskId).ifPresent(task -> {
            task.setStatus(status);
            if (volunteerId != null) {
                task.setAssignedVolunteerId(volunteerId);
                task.setAcceptedAt(Instant.now().toEpochMilli());
            }
            saveTask(task);

            // Remove from queue if accepted/completed
            if (status != TaskStatus.PENDING) {
                String queueKey = buildQueueKey(task.getPriority());
                redisTemplate.opsForZSet().remove(queueKey, String.valueOf(taskId));
            }

            // Remove from active if completed/cancelled
            if (status == TaskStatus.COMPLETED || status == TaskStatus.CANCELLED) {
                redisTemplate.opsForSet().remove(ACTIVE_TASKS_KEY, String.valueOf(taskId));
            }
        });
    }

    /**
     * Record that a volunteer was notified about a task.
     */
    public void recordNotification(Long taskId, Long volunteerId) {
        String key = buildPendingKey(taskId);
        redisTemplate.opsForSet().add(key, String.valueOf(volunteerId));
        redisTemplate.expire(key, PENDING_TTL);

        getTask(taskId).ifPresent(task -> {
            if (task.getNotifiedVolunteerIds() == null) {
                task.setNotifiedVolunteerIds(new ArrayList<>());
            }
            task.getNotifiedVolunteerIds().add(volunteerId);
            saveTask(task);
        });
    }

    /**
     * Check if volunteer was notified about a task.
     */
    public boolean wasVolunteerNotified(Long taskId, Long volunteerId) {
        String key = buildPendingKey(taskId);
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, String.valueOf(volunteerId)));
    }

    /**
     * Get tasks from queue by priority.
     */
    public List<Long> getTasksFromQueue(TaskPriority priority, int limit) {
        String queueKey = buildQueueKey(priority);
        Set<String> taskIds = redisTemplate.opsForZSet().range(queueKey, 0, limit - 1);
        if (taskIds == null)
            return Collections.emptyList();
        return taskIds.stream().map(Long::valueOf).collect(Collectors.toList());
    }

    // ==================== Volunteer Operations ====================

    /**
     * Update volunteer state.
     */
    public void updateVolunteerState(VolunteerState state) {
        try {
            state.setLastHeartbeat(Instant.now().toEpochMilli());
            String key = buildVolunteerKey(state.getVolunteerId());
            String json = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(key, json, VOLUNTEER_TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to update volunteer state {}", state.getVolunteerId(), e);
        }
    }

    /**
     * Get volunteer state.
     */
    public Optional<VolunteerState> getVolunteerState(Long volunteerId) {
        try {
            String key = buildVolunteerKey(volunteerId);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, VolunteerState.class));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to get volunteer state {}", volunteerId, e);
        }
        return Optional.empty();
    }

    /**
     * Set volunteer availability.
     */
    public void setVolunteerAvailability(Long volunteerId, VolunteerStatus status) {
        getVolunteerState(volunteerId).ifPresentOrElse(
                state -> {
                    state.setStatus(status);
                    updateVolunteerState(state);
                },
                () -> {
                    VolunteerState newState = VolunteerState.builder()
                            .volunteerId(volunteerId)
                            .status(status)
                            .build();
                    updateVolunteerState(newState);
                });
    }

    /**
     * Assign task to volunteer.
     */
    public void assignTaskToVolunteer(Long taskId, Long volunteerId) {
        updateTaskStatus(taskId, TaskStatus.ACCEPTED, volunteerId);
        setVolunteerAvailability(volunteerId, VolunteerStatus.BUSY);

        getVolunteerState(volunteerId).ifPresent(state -> {
            state.setCurrentTaskId(taskId);
            updateVolunteerState(state);
        });

        log.info("Task {} assigned to volunteer {}", taskId, volunteerId);
    }

    /**
     * Complete task and free volunteer.
     */
    public void completeTask(Long taskId) {
        getTask(taskId).ifPresent(task -> {
            updateTaskStatus(taskId, TaskStatus.COMPLETED, null);

            if (task.getAssignedVolunteerId() != null) {
                setVolunteerAvailability(task.getAssignedVolunteerId(), VolunteerStatus.AVAILABLE);
                getVolunteerState(task.getAssignedVolunteerId()).ifPresent(state -> {
                    state.setCurrentTaskId(null);
                    updateVolunteerState(state);
                });
            }
        });
    }

    /**
     * Get count of active tasks.
     */
    public long getActiveTaskCount() {
        Long count = redisTemplate.opsForSet().size(ACTIVE_TASKS_KEY);
        return count != null ? count : 0;
    }

    // ==================== Helper Methods ====================

    private void saveTask(DispatchTask task) {
        try {
            String key = buildTaskKey(task.getTaskId());
            String json = objectMapper.writeValueAsString(task);
            redisTemplate.opsForValue().set(key, json, TASK_TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to save task {}", task.getTaskId(), e);
        }
    }

    private String buildTaskKey(Long taskId) {
        return String.format("%s:%d", TASK_PREFIX, taskId);
    }

    private String buildVolunteerKey(Long volunteerId) {
        return String.format("%s:%d", VOLUNTEER_PREFIX, volunteerId);
    }

    private String buildPendingKey(Long taskId) {
        return String.format("%s:%d", PENDING_PREFIX, taskId);
    }

    private String buildQueueKey(TaskPriority priority) {
        return String.format("%s:%s", QUEUE_PREFIX, priority.name().toLowerCase());
    }
}
