package com.drc.aidbridge.data.mapper.sponsor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drc.aidbridge.data.remote.dto.request.sponsor.CreateDonationItemRequest;
import com.drc.aidbridge.data.remote.dto.request.sponsor.CreateDonationRequest;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationItem;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SponsorDonationMapper {

    @Inject
    public SponsorDonationMapper() {
    }

    public CreateDonationRequest mapToApiRequest(@Nullable SponsorDonationRequest domainModel) {
        if (domainModel == null) {
            return new CreateDonationRequest("", "", Collections.emptyList());
        }

        return new CreateDonationRequest(
            safeText(domainModel.getHubId()),
            safeText(domainModel.getNotes()),
            mapItems(domainModel.getItems())
        );
    }

    @NonNull
    private List<CreateDonationItemRequest> mapItems(@Nullable List<SponsorDonationItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<CreateDonationItemRequest> mapped = new ArrayList<>();
        for (SponsorDonationItem item : items) {
            if (item == null) {
                continue;
            }

            mapped.add(new CreateDonationItemRequest(
                safeText(item.getItemName()),
                trimToNull(item.getItemCategoryId()),
                Math.max(0, item.getQuantity()),
                safeText(item.getUnit()),
                safeText(item.getDescription()),
                trimToNull(item.getExpiryDate()),
                trimToNull(item.getImageUrl())
            ));
        }

        return mapped;
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }

    @Nullable
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
