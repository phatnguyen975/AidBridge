package com.drc.aidbridge.modules.victim.internal.usecase;

import com.drc.aidbridge.modules.aid.internal.entity.AidRequest;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestJpaRepository;
import com.drc.aidbridge.modules.shared.enums.AidStatus;
import com.drc.aidbridge.modules.shared.enums.SosStatus;
import com.drc.aidbridge.modules.shared.exception.AuthenticationException;
import com.drc.aidbridge.modules.sos.internal.entity.SosRequest;
import com.drc.aidbridge.modules.sos.internal.repository.SosJpaRepository;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.victim.internal.web.dto.VictimHistoryItemResponse;
import com.drc.aidbridge.modules.victim.internal.web.dto.VictimHistoryPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds a unified history timeline for victim from SOS and Aid requests.
 */
@Component
@RequiredArgsConstructor
public class GetVictimHistoryUseCase {

    private static final int DEFAULT_PAGE = 1;
    private static final int MAX_SIZE = 50;

    private final SosJpaRepository sosJpaRepository;
    private final AidRequestJpaRepository aidRequestJpaRepository;
    private final MissionJpaRepository missionJpaRepository;

    /**
     * Returns paginated victim history sorted by request creation time (desc).
     */
    @Transactional(readOnly = true)
    public VictimHistoryPageResponse execute(UUID requesterId, int page, int size, String timeRange, String statusFilter) {
        if (requesterId == null) {
            throw new AuthenticationException("Unauthorized request");
        }

        int safePage = Math.max(page, DEFAULT_PAGE);
        int safeSize = Math.max(1, Math.min(size, MAX_SIZE));

        Instant fromTime = resolveFromTime(timeRange);

        // 1. Fetch all missions associated with this requester
        List<Mission> sosMissions = missionJpaRepository.findMissionsBySosRequesterId(requesterId);
        List<Mission> aidMissions = missionJpaRepository.findMissionsByAidRequesterId(requesterId);
        
        // 2. Fetch all requests
        List<SosRequest> allSos = fromTime == null
            ? sosJpaRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId)
            : sosJpaRepository.findByRequesterIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(requesterId, fromTime);
        
        List<AidRequest> allAid = fromTime == null
            ? aidRequestJpaRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId)
            : aidRequestJpaRepository.findByRequesterIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(requesterId, fromTime);

        // 3. Create maps for quick lookup
        java.util.Map<UUID, Mission> sosMissionMap = sosMissions.stream()
            .collect(Collectors.toMap(Mission::getSosRequestId, m -> m, (m1, m2) -> m1));
        java.util.Map<UUID, Mission> aidMissionMap = aidMissions.stream()
            .collect(Collectors.toMap(Mission::getAidRequestId, m -> m, (m1, m2) -> m1));

        List<HistoryRecord> merged = new ArrayList<>();
        
        // Map all SOS requests, attaching mission if found
        for (SosRequest s : allSos) {
            merged.add(mapSosRecord(s, sosMissionMap.get(s.getId())));
        }
        
        // Map all Aid requests, attaching mission if found
        for (AidRequest a : allAid) {
            merged.add(mapAidRecord(a, aidMissionMap.get(a.getId())));
        }

        // Apply status filter if provided
        if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equalsIgnoreCase("all")) {
            merged = merged.stream()
                .filter(record -> record.item().getStatusType().equalsIgnoreCase(statusFilter) 
                               || record.item().getStatus().equalsIgnoreCase(statusFilter))
                .collect(Collectors.toList());
        }

        merged.sort(Comparator.comparing(HistoryRecord::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));

        long totalItems = merged.size();
        int totalPages = totalItems == 0
            ? 0
            : (int) Math.ceil((double) totalItems / safeSize);

        long fromIndexLong = (long) (safePage - 1) * safeSize;
        if (fromIndexLong >= totalItems) {
            return VictimHistoryPageResponse.builder()
                .items(Collections.emptyList())
                .page(safePage)
                .size(safeSize)
                .totalPages(totalPages)
                .totalItems(totalItems)
                .hasNext(false)
                .build();
        }

        int fromIndex = (int) fromIndexLong;
        int toIndex = Math.min(fromIndex + safeSize, merged.size());

        List<VictimHistoryItemResponse> pageItems = merged.subList(fromIndex, toIndex)
            .stream()
            .map(HistoryRecord::item)
            .collect(Collectors.toList());

        return VictimHistoryPageResponse.builder()
            .items(pageItems)
            .page(safePage)
            .size(safeSize)
            .totalPages(totalPages)
            .totalItems(totalItems)
            .hasNext(toIndex < merged.size())
            .build();
    }

    private HistoryRecord mapSosRecord(SosRequest request, Mission mission) {
        boolean relativeRequest = isRelativeSos(request != null ? request.getDescription() : null)
            || !safeText(request != null ? request.getAddress() : null).isBlank();
        
        String statusLabel;
        String statusType;
        String id;
        
        if (mission != null) {
            id = mission.getId().toString();
            statusType = resolveMissionStatusType(mission.getStatus());
            statusLabel = resolveMissionStatusLabel(mission.getStatus());
        } else {
            id = request != null && request.getId() != null ? request.getId().toString() : "";
            SosStatus status = request != null ? request.getStatus() : null;
            statusType = resolveSosStatusType(status);
            statusLabel = resolveSosStatusLabel(status);
        }

        VictimHistoryItemResponse item = VictimHistoryItemResponse.builder()
            .id(id)
            .title("")
            .status(statusLabel)
            .statusType(statusType)
            .createdAt(request != null && request.getCreatedAt() != null ? request.getCreatedAt().toString() : "")
            .location(buildLocation(request != null ? request.getAddress() : null,
                request != null ? request.getLat() : null,
                request != null ? request.getLng() : null))
            .type(relativeRequest ? "relative" : "self")
            .note(buildSosDetail(request))
            .build();

        return new HistoryRecord(request != null ? request.getCreatedAt() : null, item);
    }

    private HistoryRecord mapAidRecord(AidRequest request, Mission mission) {
        String statusLabel;
        String statusType;
        String id;
        
        if (mission != null) {
            id = mission.getId().toString();
            statusType = resolveMissionStatusType(mission.getStatus());
            statusLabel = resolveMissionStatusLabel(mission.getStatus());
        } else {
            id = request != null && request.getId() != null ? request.getId().toString() : "";
            AidStatus status = request != null ? request.getStatus() : null;
            statusType = resolveAidStatusType(status);
            statusLabel = resolveAidStatusLabel(status);
        }

        VictimHistoryItemResponse item = VictimHistoryItemResponse.builder()
            .id(id)
            .title("")
            .status(statusLabel)
            .statusType(statusType)
            .createdAt(request != null && request.getCreatedAt() != null ? request.getCreatedAt().toString() : "")
            .location(buildLocation(request != null ? request.getAddress() : null,
                request != null ? request.getLat() : null,
                request != null ? request.getLng() : null))
            .type("supply")
            .note("")
            .build();

        return new HistoryRecord(request != null ? request.getCreatedAt() : null, item);
    }

    private String buildSosDetail(SosRequest request) {
        if (request == null) {
            return "No detail";
        }

        String urgency = request.getUrgencyLevel() != null ? request.getUrgencyLevel().name() : "UNKNOWN";
        String peopleCount = request.getPeopleCount() != null ? String.valueOf(request.getPeopleCount()) : "1";
        String description = safeText(request.getDescription());

        if (description.isBlank()) {
            description = "No note";
        }

        return "Urgency: " + urgency
            + " | People: " + peopleCount
            + " | Note: " + description;
    }

    private String buildLocation(String address, BigDecimal lat, BigDecimal lng) {
        if (!safeText(address).isBlank()) {
            return safeText(address);
        }
        if (lat != null && lng != null) {
            return lat.toPlainString() + ", " + lng.toPlainString();
        }
        return "Unknown location";
    }

    private String resolveMissionStatusType(MissionStatus status) {
        if (status == null) return "STATUS_PENDING";
        return switch (status) {
            case PENDING, DISPATCHING -> "STATUS_PENDING";
            case ASSIGNED, PICKING_UP, PICKED_UP, IN_TRANSIT -> "STATUS_PROCESSING";
            case COMPLETED -> "STATUS_COMPLETED";
            case CANCELLED -> "STATUS_CANCELLED";
            default -> "STATUS_PROCESSING";
        };
    }

    private String resolveMissionStatusLabel(MissionStatus status) {
        return status != null ? status.name() : "PENDING";
    }

    private String resolveSosStatusType(SosStatus status) {
        if (status == null) return "STATUS_PENDING";
        return switch (status) {
            case PENDING -> "STATUS_PENDING";
            case COMPLETED -> "STATUS_COMPLETED";
            case CANCELLED -> "STATUS_CANCELLED";
            default -> "STATUS_PROCESSING";
        };
    }

    private String resolveAidStatusType(AidStatus status) {
        if (status == null) return "STATUS_PENDING";
        return switch (status) {
            case PENDING -> "STATUS_PENDING";
            case COMPLETED -> "STATUS_COMPLETED";
            case CANCELLED -> "STATUS_CANCELLED";
            default -> "STATUS_PROCESSING";
        };
    }

    private String resolveSosStatusLabel(SosStatus status) {
        if (status == SosStatus.CANCELLED) {
            return "Cancelled";
        }
        return "";
    }

    private String resolveAidStatusLabel(AidStatus status) {
        if (status == AidStatus.CANCELLED) {
            return "Cancelled";
        }
        return "";
    }

    private boolean isRelativeSos(String description) {
        String normalized = normalizeText(description);
        return normalized.contains("relative")
            || normalized.contains("sos nguoi than")
            || normalized.contains("loai yeu cau: sos nguoi than")
            || normalized.contains("thong tin nguoi than");
    }

    private Instant resolveFromTime(String timeRange) {
        String normalized = safeText(timeRange).toLowerCase(Locale.US);
        Instant now = Instant.now();

        return switch (normalized) {
            case "1h" -> now.minus(Duration.ofHours(1));
            case "24h" -> now.minus(Duration.ofHours(24));
            case "7d" -> now.minus(Duration.ofDays(7));
            case "1m" -> now.minus(Duration.ofDays(30));
            case "all" -> null;
            default -> now.minus(Duration.ofHours(1));
        };
    }

    private String normalizeText(String value) {
        String safeValue = safeText(value);
        if (safeValue.isBlank()) {
            return "";
        }

        return Normalizer.normalize(safeValue, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.US)
            .trim();
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }

    private record HistoryRecord(Instant createdAt, VictimHistoryItemResponse item) {
    }
}
