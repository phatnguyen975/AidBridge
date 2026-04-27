package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentSponsorDashboardBinding;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationHistoryItem;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationStatus;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.sponsor.SponsorHistoryAdapter;
import com.drc.aidbridge.ui.main.viewmodel.sponsor.SponsorHistoryViewModel;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorDashboardFragment extends BaseFragment<FragmentSponsorDashboardBinding> {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
                    .withZone(ZoneId.systemDefault());

    private SponsorHistoryAdapter sponsorHistoryAdapter;
    private SponsorHistoryViewModel historyViewModel;

    @Nullable
    @Override
    protected FragmentSponsorDashboardBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorDashboardBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        historyViewModel = new ViewModelProvider(this).get(SponsorHistoryViewModel.class);

        binding.cardRegisterDonation.setOnClickListener(v -> navigateToDestinationSafely(R.id.sponsorDonateFragment));
        binding.cardDonationHistory.setOnClickListener(v -> navigateToDestinationSafely(R.id.sponsorHistoryFragment));

        // Dùng SponsorHistoryAdapter để đồng nhất giao diện thẻ card
        sponsorHistoryAdapter = new SponsorHistoryAdapter(this::onHistoryItemClicked);
        binding.rvRecentDonations.setAdapter(sponsorHistoryAdapter);

        // Load 4 mục gần nhất
        historyViewModel.loadInitialHistory(null);
    }

    @Override
    protected void observeViewModel() {
        historyViewModel.getHistoryItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                // Chỉ lấy tối đa 4 item cho trang chủ
                List<SponsorDonationHistoryItem> recentItems = items.subList(0, Math.min(items.size(), 4));
                sponsorHistoryAdapter.submitItems(mapToUiItems(recentItems));
            }
        });

        historyViewModel.getHistoryResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                binding.progressBar.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void onHistoryItemClicked(@NonNull SponsorHistoryAdapter.HistoryItem item) {
        if (!"REGISTERED".equalsIgnoreCase(item.statusKey)) {
            return;
        }

        String donationCode = safeText(item.donationCode);
        if (donationCode.isEmpty()) {
            donationCode = getString(R.string.sponsor_qr_missing_value);
        }

        String itemSummary = safeText(item.itemSummary);
        if (itemSummary.isEmpty()) {
            itemSummary = getString(R.string.sponsor_qr_missing_value);
        }

        String quantityText = safeText(item.quantity);
        if (quantityText.isEmpty()) {
            quantityText = getString(R.string.sponsor_qr_missing_value);
        }

        Bundle args = new Bundle();
        args.putString(SponsorQrCodeFragment.ARG_DONATION_CODE, donationCode);
        args.putString(SponsorQrCodeFragment.ARG_QR_CODE_TOKEN, item.qrCodeToken);
        args.putString(SponsorQrCodeFragment.ARG_ITEM_NAME, itemSummary);
        args.putString(SponsorQrCodeFragment.ARG_QUANTITY_TEXT, quantityText);
        navigateSafely(R.id.action_sponsor_dashboard_to_qr, args);
    }

    @NonNull
    private List<SponsorHistoryAdapter.HistoryItem> mapToUiItems(@Nullable List<SponsorDonationHistoryItem> source) {
        List<SponsorHistoryAdapter.HistoryItem> mapped = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            return mapped;
        }

        for (SponsorDonationHistoryItem item : source) {
            if (item == null || item.getStatus() == null) {
                continue;
            }

            String donationCode = safeText(item.getDonationCode());
            String displayDonationCode = !donationCode.isEmpty()
                    ? donationCode
                    : getString(R.string.sponsor_qr_missing_value);

            String itemSummary = safeText(item.getItemSummary());
            String displayItemSummary = !itemSummary.isEmpty()
                    ? itemSummary
                    : getString(R.string.sponsor_qr_missing_value);

            String dateText = formatDate(item.getCreatedAt());

            int itemCount = item.getItemCount();
            String quantityText = itemCount > 0
                    ? getString(R.string.sponsor_history_item_count_value, itemCount)
                    : getString(R.string.sponsor_qr_missing_value);

            String hubRawText = safeText(item.getHubId());
            String hubText = !hubRawText.isEmpty()
                    ? hubRawText
                    : getString(R.string.sponsor_hub_unknown_name);

            String statusKey = item.getStatus().name();
            String statusLabel = getStatusLabel(item.getStatus());

            mapped.add(new SponsorHistoryAdapter.HistoryItem(
                    safeText(item.getId()),
                    dateText,
                    displayItemSummary,
                    quantityText,
                    hubText,
                    statusKey,
                    statusLabel,
                    displayItemSummary,
                    displayDonationCode,
                    safeText(item.getQrCodeToken()),
                    R.mipmap.ic_launcher
            ));
        }

        return mapped;
    }

    @NonNull
    private String getStatusLabel(@NonNull SponsorDonationStatus status) {
        if (status == SponsorDonationStatus.REGISTERED) {
            return getString(R.string.sponsor_history_status_registered);
        }
        if (status == SponsorDonationStatus.RECEIVED) {
            return getString(R.string.sponsor_history_status_received);
        }
        return getString(R.string.sponsor_history_status_outdated);
    }

    @NonNull
    private String formatDate(@Nullable String isoDateTime) {
        String safeIsoDateTime = safeText(isoDateTime);
        if (safeIsoDateTime.isEmpty()) {
            return getString(R.string.sponsor_qr_missing_value);
        }

        try {
            Instant instant = Instant.parse(safeIsoDateTime);
            return DATE_FORMATTER.format(instant);
        } catch (DateTimeParseException exception) {
            return safeIsoDateTime;
        }
    }

    @NonNull
    private String safeText(@Nullable String value) {
        return value != null ? value.trim() : "";
    }
}
