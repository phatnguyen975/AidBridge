package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentSponsorHistoryBinding;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationHistoryItem;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationStatus;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.sponsor.SponsorHistoryAdapter;
import com.drc.aidbridge.ui.main.viewmodel.sponsor.SponsorHistoryViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorHistoryFragment extends BaseFragment<FragmentSponsorHistoryBinding> {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
                    .withZone(ZoneId.systemDefault());

    private SponsorHistoryAdapter sponsorHistoryAdapter;
    private SponsorHistoryViewModel viewModel;
    private int baseRecyclerBottomPadding;

    @Nullable
    @Override
    protected FragmentSponsorHistoryBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorHistoryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(SponsorHistoryViewModel.class);
        binding.ivBack.setOnClickListener(v -> popBackStackSafely());

        sponsorHistoryAdapter = new SponsorHistoryAdapter(this::onHistoryItemClicked);
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(sponsorHistoryAdapter);
        baseRecyclerBottomPadding = binding.rvHistory.getPaddingBottom();
        binding.rvHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy <= 0) {
                    return;
                }

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) {
                    return;
                }

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                    viewModel.loadNextPage();
                }
            }
        });

        setupTabs();
    }

    @Override
    protected void observeViewModel() {
        if (viewModel == null) {
            return;
        }

        viewModel.getHistoryItems().observe(getViewLifecycleOwner(), items -> {
            sponsorHistoryAdapter.submitItems(mapToUiItems(items));
        });

        viewModel.getHistoryResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) {
                return;
            }

            if (result.isLoading()) {
                updatePaginationLoading(true);
                return;
            }

            updatePaginationLoading(false);

            if (result.isError() && !result.hasBeenHandled()) {
                result.markAsHandled();
                showTopSnackbar(
                        binding.getRoot(),
                        result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic),
                        true
                );
                return;
            }

            if (result.isSuccess() && !result.hasBeenHandled()) {
                if (result.getData() == null) {
                    return;
                }

                boolean isFirstPage = result.getData().getPage() <= 1;
                boolean isEmptyPage = result.getData().getItems() == null || result.getData().getItems().isEmpty();
                if (!isFirstPage || !isEmptyPage) {
                    return;
                }

                result.markAsHandled();
                showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_history_empty), false);
            }
        });
    }

    private void setupTabs() {
        addAllTab();
        addStatusTab(SponsorDonationStatus.REGISTERED, R.string.sponsor_history_status_registered);
        addStatusTab(SponsorDonationStatus.RECEIVED, R.string.sponsor_history_status_received);
        addStatusTab(SponsorDonationStatus.OUTDATED, R.string.sponsor_history_status_outdated);

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab == null) {
                    return;
                }

                SponsorDonationStatus selectedStatus =
                        tab.getTag() instanceof SponsorDonationStatus
                                ? (SponsorDonationStatus) tab.getTag()
                                : null;
                viewModel.loadInitialHistory(selectedStatus);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // No-op
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // No-op
            }
        });

        // Tab đầu có thể đã được auto-selected khi addTab, nên gọi API chủ động để đảm bảo luôn load lần đầu.
        TabLayout.Tab initialTab = binding.tabLayout.getTabAt(0);
        SponsorDonationStatus initialStatus = null;
        if (initialTab != null && initialTab.getTag() instanceof SponsorDonationStatus) {
            initialStatus = (SponsorDonationStatus) initialTab.getTag();
        }
        viewModel.loadInitialHistory(initialStatus);

        if (initialTab != null && !initialTab.isSelected()) {
            initialTab.select();
        }
    }

    private void addAllTab() {
        TabLayout.Tab tab = binding.tabLayout.newTab().setText(getString(R.string.sponsor_history_tab_all));
        tab.setTag(null);
        binding.tabLayout.addTab(tab);
    }

    private void onHistoryItemClicked(@NonNull SponsorHistoryAdapter.HistoryItem item) {
        if (!"REGISTERED".equalsIgnoreCase(item.statusKey)) {
            return;
        }

        Bundle args = new Bundle();
        args.putString(SponsorQrCodeFragment.ARG_DONATION_CODE, item.donationCode);
        args.putString(SponsorQrCodeFragment.ARG_QR_CODE_TOKEN, item.qrCodeToken);
        args.putString(SponsorQrCodeFragment.ARG_ITEM_NAME, item.itemSummary);
        args.putString(SponsorQrCodeFragment.ARG_QUANTITY_TEXT, item.quantity);
        navigateSafely(R.id.action_sponsor_history_to_qr, args);
    }

    private void addStatusTab(@NonNull SponsorDonationStatus status, int titleRes) {
        TabLayout.Tab tab = binding.tabLayout.newTab().setText(getString(titleRes));
        tab.setTag(status);
        binding.tabLayout.addTab(tab);
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

            String hubId = safeText(item.getHubId());
            String hubText = !hubId.isEmpty()
                    ? getString(R.string.sponsor_history_hub_short_value, shortenHubId(hubId))
                    : getString(R.string.sponsor_hub_unknown_name);

            String statusKey = item.getStatus().name();
            String statusLabel = getStatusLabel(item.getStatus());

            mapped.add(new SponsorHistoryAdapter.HistoryItem(
                    safeText(item.getId()),
                    dateText,
                    displayDonationCode,
                    quantityText,
                    hubText,
                    statusKey,
                    statusLabel,
                    displayItemSummary,
                    donationCode,
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

    private void updatePaginationLoading(boolean show) {
        boolean hasLoadedItems = sponsorHistoryAdapter != null && sponsorHistoryAdapter.getItemCount() > 0;

        if (!show) {
            binding.initialLoadingProgress.setVisibility(View.GONE);
            binding.paginationProgress.setVisibility(View.GONE);
            setTemporaryBottomSpace(false);
            return;
        }

        binding.initialLoadingProgress.setVisibility(hasLoadedItems ? View.GONE : View.VISIBLE);
        binding.paginationProgress.setVisibility(hasLoadedItems ? View.VISIBLE : View.GONE);
        setTemporaryBottomSpace(hasLoadedItems);
    }

    private void setTemporaryBottomSpace(boolean enabled) {
        int extraSpace = enabled ? getResources().getDimensionPixelSize(R.dimen.spacing_xxl) : 0;
        binding.rvHistory.setPaddingRelative(
                binding.rvHistory.getPaddingStart(),
                binding.rvHistory.getPaddingTop(),
                binding.rvHistory.getPaddingEnd(),
                baseRecyclerBottomPadding + extraSpace
        );
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
    private String shortenHubId(@NonNull String hubId) {
        if (hubId.length() <= 8) {
            return hubId;
        }
        return hubId.substring(0, 8);
    }

    @NonNull
    private String safeText(@Nullable String value) {
        return value != null ? value.trim() : "";
    }
}
