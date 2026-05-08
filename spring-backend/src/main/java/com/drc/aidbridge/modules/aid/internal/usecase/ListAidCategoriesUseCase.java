package com.drc.aidbridge.modules.aid.internal.usecase;

import com.drc.aidbridge.modules.aid.internal.repository.AidRequestItemJpaRepository;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidCategoryItemResponse;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidCategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Returns 2-level aid categories (parent + child) for victim supply form.
 */
@Component
@RequiredArgsConstructor
public class ListAidCategoriesUseCase {

    private final AidRequestItemJpaRepository aidRequestItemJpaRepository;

    /**
     * Reads category tree from item_categories and groups children by parent.
     */
    @Transactional(readOnly = true)
    public List<AidCategoryResponse> execute() {
        List<AidRequestItemJpaRepository.AidCategoryProjection> rows = aidRequestItemJpaRepository
                .findAidCategoryRows();

        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        Map<UUID, CategoryAggregate> categoryMap = new LinkedHashMap<>();
        for (AidRequestItemJpaRepository.AidCategoryProjection row : rows) {
            if (row == null || row.getParentId() == null) {
                continue;
            }

            CategoryAggregate aggregate = categoryMap.computeIfAbsent(
                    row.getParentId(),
                    ignored -> new CategoryAggregate(row.getParentId(), safeText(row.getParentName())));
        }

        List<AidCategoryResponse> result = new ArrayList<>();
        for (CategoryAggregate aggregate : categoryMap.values()) {
            result.add(
                    AidCategoryResponse.builder()
                            .id(aggregate.id)
                            .name(aggregate.name)
                            .items(aggregate.items)
                            .build());
        }

        return result;
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }

    private static final class CategoryAggregate {
        private final UUID id;
        private final String name;
        private final List<AidCategoryItemResponse> items = new ArrayList<>();

        private CategoryAggregate(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
