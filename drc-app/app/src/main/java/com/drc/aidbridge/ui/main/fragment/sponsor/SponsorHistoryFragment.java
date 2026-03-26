package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentSponsorHistoryBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.sponsor.SponsorHistoryAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorHistoryFragment extends BaseFragment<FragmentSponsorHistoryBinding> {

    private SponsorHistoryAdapter sponsorHistoryAdapter;
    private List<SponsorHistoryAdapter.HistoryItem> allMockItems;

    @Nullable
    @Override
    protected FragmentSponsorHistoryBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorHistoryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        binding.ivBack.setOnClickListener(v -> popBackStackSafely());

        sponsorHistoryAdapter = new SponsorHistoryAdapter(this::onHistoryItemClicked);
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(sponsorHistoryAdapter);

        setupTabs();
        loadMockData();
    }

    @Override
    protected void observeViewModel() {
    }

    private void setupTabs() {
        String[] tabs = new String[] {
                getString(R.string.sponsor_history_tab_all),
                getString(R.string.sponsor_history_status_pending),
                getString(R.string.sponsor_history_status_stocked),
                getString(R.string.sponsor_history_status_shipping),
                getString(R.string.sponsor_history_status_arrived)
        };

        for (String tabLabel : tabs) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(tabLabel));
        }

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab == null || tab.getText() == null) {
                    return;
                }
                sponsorHistoryAdapter.submitItems(filterItemsByTab(tab.getText().toString()));
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
    }

    private void onHistoryItemClicked(@NonNull SponsorHistoryAdapter.HistoryItem item) {
        if (item.status.equals(getString(R.string.sponsor_history_status_pending))) {
            navigateToDestinationSafely(R.id.sponsorQrCodeFragment);
        }
    }

    private void loadMockData() {
        // TODO: Replace with ViewModel observation when history API is available.
        allMockItems = buildMockHistoryItems();
        sponsorHistoryAdapter.submitItems(allMockItems);
    }

    @NonNull
    private List<SponsorHistoryAdapter.HistoryItem> filterItemsByTab(@NonNull String tabText) {
        if (allMockItems == null || tabText.equals(getString(R.string.sponsor_history_tab_all))) {
            return allMockItems == null ? new ArrayList<>() : new ArrayList<>(allMockItems);
        }

        List<SponsorHistoryAdapter.HistoryItem> filteredItems = new ArrayList<>();
        for (SponsorHistoryAdapter.HistoryItem item : allMockItems) {
            if (item.status.equals(tabText)) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

    @NonNull
    private List<SponsorHistoryAdapter.HistoryItem> buildMockHistoryItems() {
        List<SponsorHistoryAdapter.HistoryItem> items = new ArrayList<>();
        items.add(new SponsorHistoryAdapter.HistoryItem(
                "DON-001",
                "24/05/2024",
                getString(R.string.sponsor_donate_category_food),
                "50 thùng",
                "Trạm Quận 7",
                getString(R.string.sponsor_history_status_pending),
                R.mipmap.ic_launcher
        ));
        items.add(new SponsorHistoryAdapter.HistoryItem(
                "DON-002",
                "21/05/2024",
                getString(R.string.sponsor_donate_category_water),
                "120 thùng",
                "Trạm Bình Thạnh",
                getString(R.string.sponsor_history_status_stocked),
                R.mipmap.ic_launcher
        ));
        items.add(new SponsorHistoryAdapter.HistoryItem(
                "DON-003",
                "19/05/2024",
                getString(R.string.sponsor_donate_category_medicine),
                "30 kiện",
                "Trạm Thủ Đức",
                getString(R.string.sponsor_history_status_shipping),
                R.mipmap.ic_launcher
        ));
        items.add(new SponsorHistoryAdapter.HistoryItem(
                "DON-004",
                "16/05/2024",
                getString(R.string.sponsor_donate_category_clothes),
                "200 bộ",
                "Trạm Gò Vấp",
                getString(R.string.sponsor_history_status_arrived),
                R.mipmap.ic_launcher
        ));
        items.add(new SponsorHistoryAdapter.HistoryItem(
                "DON-005",
                "15/05/2024",
                getString(R.string.sponsor_donate_category_food),
                "80 suất",
                "Trạm Quận 12",
                getString(R.string.sponsor_history_status_pending),
                R.mipmap.ic_launcher
        ));
        items.add(new SponsorHistoryAdapter.HistoryItem(
                "DON-006",
                "12/05/2024",
                getString(R.string.sponsor_donate_category_water),
                "90 bình",
                "Trạm Tân Bình",
                getString(R.string.sponsor_history_status_arrived),
                R.mipmap.ic_launcher
        ));
        return items;
    }
}
