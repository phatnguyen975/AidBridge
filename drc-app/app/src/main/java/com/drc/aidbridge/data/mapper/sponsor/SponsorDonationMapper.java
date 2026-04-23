package com.drc.aidbridge.data.mapper.sponsor;

import androidx.annotation.Nullable;

import com.drc.aidbridge.data.remote.dto.request.sponsor.CreateDonationItemRequest;
import com.drc.aidbridge.data.remote.dto.request.sponsor.CreateDonationRequest;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationRequest;

import java.util.Collections;

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

        CreateDonationItemRequest itemRequest = new CreateDonationItemRequest(
            safeText(domainModel.getItemName()),
            Math.max(0, domainModel.getQuantity()),
            safeText(domainModel.getUnit()),
            safeText(domainModel.getDescription()),
            trimToNull(domainModel.getImageUrl())
        );

        String notes = buildNotes(
            domainModel.getCategory(),
            domainModel.getDescription(),
            domainModel.getExpectedTime()
        );

        return new CreateDonationRequest(
            safeText(domainModel.getHubId()),
            notes,
            Collections.singletonList(itemRequest)
        );
    }

    private String buildNotes(String category, String description, String expectedTime) {
        String safeCategory = safeText(category);
        String safeDescription = safeText(description);
        String safeExpectedTime = safeText(expectedTime);

        StringBuilder notesBuilder = new StringBuilder();
        if (!safeCategory.isEmpty()) {
            notesBuilder.append("Category: ").append(safeCategory);
        }

        if (!safeDescription.isEmpty()) {
            if (notesBuilder.length() > 0) {
                notesBuilder.append(" | ");
            }
            notesBuilder.append(safeDescription);
        }

        if (!safeExpectedTime.isEmpty()) {
            if (notesBuilder.length() > 0) {
                notesBuilder.append(" | ");
            }
            notesBuilder.append("Expected time: ").append(safeExpectedTime);
        }

        return notesBuilder.toString();
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
