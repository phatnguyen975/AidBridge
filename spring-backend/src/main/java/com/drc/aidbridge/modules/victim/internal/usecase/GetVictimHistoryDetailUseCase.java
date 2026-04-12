package com.drc.aidbridge.modules.victim.internal.usecase;

import com.drc.aidbridge.modules.aid.internal.entity.AidRequest;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestItemJpaRepository;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestJpaRepository;
import com.drc.aidbridge.modules.shared.enums.AidStatus;
import com.drc.aidbridge.modules.shared.enums.SosStatus;
import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import com.drc.aidbridge.modules.shared.exception.AuthenticationException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.sos.internal.entity.SosRequest;
import com.drc.aidbridge.modules.sos.internal.repository.SosJpaRepository;
import com.drc.aidbridge.modules.victim.internal.web.dto.VictimHistoryAidItemDetailResponse;
import com.drc.aidbridge.modules.victim.internal.web.dto.VictimHistoryDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves detail payload for a single history request of the authenticated victim.
 */
@Component
@RequiredArgsConstructor
public class GetVictimHistoryDetailUseCase {

    private static final Pattern NAME_PATTERN = Pattern.compile("Họ tên\\s*:\\s*([^.]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("Số điện thoại liên hệ\\s*:\\s*([^.]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEALTH_PATTERN = Pattern.compile("Chi tiết sức khỏe\\s*:\\s*([^.]*)", Pattern.CASE_INSENSITIVE);

    private final SosJpaRepository sosJpaRepository;
    private final AidRequestJpaRepository aidRequestJpaRepository;
    private final AidRequestItemJpaRepository aidRequestItemJpaRepository;

    /**
     * Gets detail information for SOS or aid request by request id and request type.
     */
    @Transactional(readOnly = true)
    public VictimHistoryDetailResponse execute(UUID requesterId, UUID requestId, String type) {
        if (requesterId == null) {
            throw new AuthenticationException("Unauthorized request");
        }
        if (requestId == null) {
            throw new ResourceNotFoundException("History request id is required");
        }

        String normalizedType = safeText(type).toLowerCase(Locale.US);
        if (normalizedType.contains("supply") || normalizedType.contains("relief")) {
            return buildAidDetail(requesterId, requestId);
        }

        if (normalizedType.contains("self") || normalizedType.contains("relative")
            || normalizedType.contains("sos")) {
            return buildSosDetail(requesterId, requestId);
        }

        return sosJpaRepository.findByIdAndRequesterId(requestId, requesterId)
            .map(this::mapSosDetail)
            .orElseGet(() -> buildAidDetail(requesterId, requestId));
    }

    private VictimHistoryDetailResponse buildSosDetail(UUID requesterId, UUID requestId) {
        SosRequest request = sosJpaRepository.findByIdAndRequesterId(requestId, requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("SOS request not found: " + requestId));
        return mapSosDetail(request);
    }

    private VictimHistoryDetailResponse mapSosDetail(SosRequest request) {
        String description = request != null ? safeText(request.getDescription()) : "";
        boolean relativeRequest = isRelativeSos(description)
            || !safeText(request != null ? request.getAddress() : null).isBlank();

        return VictimHistoryDetailResponse.builder()
            .id(request != null && request.getId() != null ? request.getId().toString() : "")
            .type(relativeRequest ? "relative" : "self")
            .status(resolveSosStatusLabel(request != null ? request.getStatus() : null))
            .statusType(resolveSosStatusType(request != null ? request.getStatus() : null))
            .createdAt(request != null && request.getCreatedAt() != null ? request.getCreatedAt().toString() : "")
            .location(buildLocation(request != null ? request.getAddress() : null,
                request != null ? request.getLat() : null,
                request != null ? request.getLng() : null))
            .condition(resolveUrgencyLabel(request != null ? request.getUrgencyLevel() : null))
            .peopleCount(request != null ? request.getPeopleCount() : null)
            .noteFullName(extractDescriptionValue(NAME_PATTERN, description))
            .notePhoneNumber(extractDescriptionValue(PHONE_PATTERN, description))
            .noteHealthDetail(extractDescriptionValue(HEALTH_PATTERN, description))
            .requestedItems(Collections.emptyList())
            .build();
    }

    private VictimHistoryDetailResponse buildAidDetail(UUID requesterId, UUID requestId) {
        AidRequest request = aidRequestJpaRepository.findByIdAndRequesterId(requestId, requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Aid request not found: " + requestId));

        List<AidRequestItemJpaRepository.AidRequestItemDetailProjection> itemRows =
            aidRequestItemJpaRepository.findDetailRowsByAidRequestId(requestId);

        return VictimHistoryDetailResponse.builder()
            .id(request.getId() != null ? request.getId().toString() : "")
            .type("supply")
            .status(resolveAidStatusLabel(request.getStatus()))
            .statusType(resolveAidStatusType(request.getStatus()))
            .createdAt(request.getCreatedAt() != null ? request.getCreatedAt().toString() : "")
            .location(buildLocation(request.getAddress(), request.getLat(), request.getLng()))
            .numberAdult(request.getNumberAdult())
            .numberElderly(request.getNumberElderly())
            .numberChildren(request.getNumberChildren())
            .requestedItems(mapAidItems(itemRows))
            .build();
    }

    private List<VictimHistoryAidItemDetailResponse> mapAidItems(
        List<AidRequestItemJpaRepository.AidRequestItemDetailProjection> itemRows
    ) {
        if (itemRows == null || itemRows.isEmpty()) {
            return Collections.emptyList();
        }

        List<VictimHistoryAidItemDetailResponse> items = new ArrayList<>();
        for (AidRequestItemJpaRepository.AidRequestItemDetailProjection row : itemRows) {
            if (row == null) {
                continue;
            }

            long itemCount = row.getItemCount() != null ? row.getItemCount() : 0L;
            int quantity = itemCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, itemCount);

            items.add(VictimHistoryAidItemDetailResponse.builder()
                .categoryName(safeText(row.getCategoryName()))
                .quantity(quantity)
                .unit(safeText(row.getUnit()))
                .build());
        }
        return items;
    }

    private String extractDescriptionValue(Pattern pattern, String description) {
        String safeDescription = safeText(description);
        if (safeDescription.isBlank()) {
            return "";
        }

        Matcher matcher = pattern.matcher(safeDescription);
        if (!matcher.find()) {
            return "";
        }

        return safeText(matcher.group(1));
    }

    private String buildLocation(String address, BigDecimal lat, BigDecimal lng) {
        if (!safeText(address).isBlank()) {
            return safeText(address);
        }
        if (lat != null && lng != null) {
            return lat.toPlainString() + ", " + lng.toPlainString();
        }
        return "";
    }

    private String resolveSosStatusType(SosStatus status) {
        if (status == null) {
            return "processing";
        }

        return switch (status) {
            case PENDING -> "pending";
            case COMPLETED -> "completed";
            default -> "processing";
        };
    }

    private String resolveAidStatusType(AidStatus status) {
        if (status == null) {
            return "processing";
        }

        return switch (status) {
            case PENDING -> "pending";
            case COMPLETED -> "completed";
            default -> "processing";
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

    private String resolveUrgencyLabel(UrgencyLevel urgencyLevel) {
        if (urgencyLevel == null) {
            return "";
        }

        return switch (urgencyLevel) {
            case CRITICAL -> "Nguy kịch";
            case HIGH -> "Nghiêm trọng";
            case MEDIUM -> "Trung bình";
            case LOW -> "Nhẹ";
        };
    }

    private boolean isRelativeSos(String description) {
        String normalized = normalizeText(description);
        return normalized.contains("relative")
            || normalized.contains("sos nguoi than")
            || normalized.contains("loai yeu cau: sos nguoi than")
            || normalized.contains("thong tin nguoi than");
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

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }
}
