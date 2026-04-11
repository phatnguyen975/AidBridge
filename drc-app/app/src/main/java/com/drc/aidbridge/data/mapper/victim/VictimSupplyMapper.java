package com.drc.aidbridge.data.mapper.victim;

import androidx.annotation.Nullable;

import com.drc.aidbridge.data.remote.dto.request.victim.ReliefRequest;
import com.drc.aidbridge.data.remote.dto.request.victim.RequestedItemRequest;
import com.drc.aidbridge.data.remote.dto.response.victim.SupplyCategoryResponse;
import com.drc.aidbridge.data.remote.dto.response.victim.SupplyItemResponse;
import com.drc.aidbridge.domain.model.victim.VictimReliefRequest;
import com.drc.aidbridge.domain.model.victim.VictimRequestedItem;
import com.drc.aidbridge.domain.model.victim.VictimSupplyCategory;
import com.drc.aidbridge.domain.model.victim.VictimSupplyItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VictimSupplyMapper {

    @Inject
    public VictimSupplyMapper() {
    }

    public List<VictimSupplyCategory> mapCategoriesToDomain(@Nullable List<SupplyCategoryResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return Collections.emptyList();
        }

        List<VictimSupplyCategory> result = new ArrayList<>();
        for (SupplyCategoryResponse response : responses) {
            if (response == null) {
                continue;
            }

            result.add(new VictimSupplyCategory(
                safeText(response.getId()),
                safeText(response.getName()),
                mapItemsToDomain(response.getItems())
            ));
        }
        return result;
    }

    public ReliefRequest mapReliefRequestToRequest(@Nullable VictimReliefRequest domainModel) {
        if (domainModel == null) {
            return new ReliefRequest(0, 0, 0, "", Collections.emptyList());
        }

        return new ReliefRequest(
            domainModel.getAdultsCount(),
            domainModel.getEldersCount(),
            domainModel.getChildrenCount(),
            safeText(domainModel.getNote()),
            mapRequestedItemsToRequest(domainModel.getRequestedItems())
        );
    }

    private List<VictimSupplyItem> mapItemsToDomain(@Nullable List<SupplyItemResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return Collections.emptyList();
        }

        List<VictimSupplyItem> result = new ArrayList<>();
        for (SupplyItemResponse response : responses) {
            if (response == null) {
                continue;
            }
            result.add(new VictimSupplyItem(safeText(response.getId()), safeText(response.getName())));
        }
        return result;
    }

    private List<RequestedItemRequest> mapRequestedItemsToRequest(@Nullable List<VictimRequestedItem> domainItems) {
        if (domainItems == null || domainItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<RequestedItemRequest> requests = new ArrayList<>();
        for (VictimRequestedItem item : domainItems) {
            if (item == null) {
                continue;
            }

            String itemId = safeText(item.getItemId());
            int quantity = item.getQuantity();
            if (itemId.isEmpty() || quantity <= 0) {
                continue;
            }

            requests.add(new RequestedItemRequest(itemId, quantity));
        }
        return requests;
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }
}
