package com.drc.aidbridge.data.mapper.victim;

import androidx.annotation.Nullable;

import com.drc.aidbridge.data.local.entity.VictimHistoryEntity;
import com.drc.aidbridge.data.remote.dto.response.victim.HistoryResponse;
import com.drc.aidbridge.domain.model.victim.VictimHistoryItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VictimHistoryMapper {

    @Inject
    public VictimHistoryMapper() {
    }

    public List<VictimHistoryItem> mapResponsesToDomain(@Nullable List<HistoryResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return Collections.emptyList();
        }

        List<VictimHistoryItem> items = new ArrayList<>();
        for (HistoryResponse response : responses) {
            if (response == null) {
                continue;
            }
            items.add(mapResponseToDomain(response));
        }
        return items;
    }

    public VictimHistoryItem mapResponseToDomain(@Nullable HistoryResponse response) {
        if (response == null) {
            return new VictimHistoryItem("", "", "", "", "", "", "", "");
        }

        return new VictimHistoryItem(
            safeText(response.getRequestId()),
            safeText(response.getTitle()),
            safeText(response.getStatus()),
            safeText(response.getStatusType()),
            safeText(response.getDateTime()),
            safeText(response.getLocation()),
            safeText(response.getType()),
            safeText(response.getDetail())
        );
    }

    public List<VictimHistoryItem> mapEntitiesToDomain(@Nullable List<VictimHistoryEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        List<VictimHistoryItem> items = new ArrayList<>();
        for (VictimHistoryEntity entity : entities) {
            if (entity == null) {
                continue;
            }

            items.add(new VictimHistoryItem(
                safeText(entity.requestId),
                safeText(entity.title),
                safeText(entity.status),
                safeText(entity.statusType),
                safeText(entity.dateTime),
                safeText(entity.location),
                safeText(entity.type),
                safeText(entity.detail)
            ));
        }
        return items;
    }

    public List<VictimHistoryEntity> mapResponsesToEntities(@Nullable List<HistoryResponse> responses,
                                                            String timeRange,
                                                            int page,
                                                            boolean hasNextPage,
                                                            long cachedAt) {
        if (responses == null || responses.isEmpty()) {
            return Collections.emptyList();
        }

        List<VictimHistoryEntity> entities = new ArrayList<>();
        for (int index = 0; index < responses.size(); index++) {
            HistoryResponse response = responses.get(index);
            if (response == null) {
                continue;
            }

            String requestId = safeText(response.getRequestId());
            if (requestId.isEmpty()) {
                requestId = "history_" + page + "_" + index;
            }

            String cacheKey = requestId + "|" + timeRange + "|" + page;

            entities.add(new VictimHistoryEntity(
                cacheKey,
                requestId,
                safeText(response.getTitle()),
                safeText(response.getStatus()),
                safeText(response.getStatusType()),
                safeText(response.getDateTime()),
                safeText(response.getLocation()),
                safeText(response.getType()),
                safeText(response.getDetail()),
                timeRange,
                page,
                index,
                hasNextPage,
                cachedAt
            ));
        }
        return entities;
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }
}
