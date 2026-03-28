package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private int baseRecyclerBottomPadding;

    private int currentPage = 1;
    private String currentFilterTab = "Tất cả";
    private boolean isLoading = false;
    private boolean isLastPage = false;

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
        baseRecyclerBottomPadding = binding.rvHistory.getPaddingBottom();
        binding.rvHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy <= 0 || isLoading || isLastPage) {
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
                    currentPage++;
                    loadMockData();
                }
            }
        });

        setupTabs();
        currentFilterTab = getString(R.string.sponsor_history_tab_all);
        loadMockData();
    }

    @Override
    protected void observeViewModel() {
        // TODO: Observe real data from ViewModel when implemented
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

                currentFilterTab = tab.getText().toString();
                currentPage = 1;
                isLastPage = false;
                sponsorHistoryAdapter.clear();
                loadMockData();
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
        if (isLoading || isLastPage) {
            return;
        }

        // TODO(API): Remove mock pagination delay and call ViewModel paged history endpoint.
        isLoading = true;
        updatePaginationLoading(true);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            List<SponsorHistoryAdapter.HistoryItem> pageData = buildMockHistoryItems(currentPage, currentFilterTab);

            if (pageData.isEmpty()) {
                isLastPage = true;
            } else {
                sponsorHistoryAdapter.addItems(pageData);
                if (currentPage >= 3) {
                    isLastPage = true;
                }
            }

            isLoading = false;
            if (binding != null) {
                updatePaginationLoading(false);
            }
        }, 500);
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
    private List<SponsorHistoryAdapter.HistoryItem> buildMockHistoryItems(int page, @NonNull String statusFilter) {
        // TODO(API): Remove this mock generator and map paged API response items.
        List<SponsorHistoryAdapter.HistoryItem> items = new ArrayList<>();
        String statusPending = getString(R.string.sponsor_history_status_pending);
        String statusStocked = getString(R.string.sponsor_history_status_stocked);
        String statusShipping = getString(R.string.sponsor_history_status_shipping);
        String statusArrived = getString(R.string.sponsor_history_status_arrived);

        List<SponsorHistoryAdapter.HistoryItem> pageItems = new ArrayList<>();
        pageItems.add(new SponsorHistoryAdapter.HistoryItem(
                "DON-P" + page + "-01",
                "24/05/2024",
                getString(R.string.sponsor_donate_category_food),
                "50 thùng",
                "Trạm Quận 7",
                statusPending,
                R.mipmap.ic_launcher
        ));
        pageItems.add(new SponsorHistoryAdapter.HistoryItem(
                "DON-P" + page + "-02",
                "21/05/2024",
                getString(R.string.sponsor_donate_category_water),
                "120 thùng",
                "Trạm Bình Thạnh",
                statusStocked,
                R.mipmap.ic_launcher
        ));
        pageItems.add(new SponsorHistoryAdapter.HistoryItem(
                "DON-P" + page + "-03",
                "19/05/2024",
                getString(R.string.sponsor_donate_category_medicine),
                "30 kiện",
                "Trạm Thủ Đức",
                statusShipping,
                R.mipmap.ic_launcher
        ));
        pageItems.add(new SponsorHistoryAdapter.HistoryItem(
                "DON-P" + page + "-04",
                "16/05/2024",
                getString(R.string.sponsor_donate_category_clothes),
                "200 bộ",
                "Trạm Gò Vấp",
                statusArrived,
                R.mipmap.ic_launcher
        ));
        pageItems.add(new SponsorHistoryAdapter.HistoryItem(
                "DON-P" + page + "-05",
                "15/05/2024",
                getString(R.string.sponsor_donate_category_food),
                "80 suất",
                "Trạm Quận 12",
                statusPending,
                R.mipmap.ic_launcher
        ));
        pageItems.add(new SponsorHistoryAdapter.HistoryItem(
                "DON-P" + page + "-06",
                "12/05/2024",
                getString(R.string.sponsor_donate_category_water),
                "90 bình",
                "Trạm Tân Bình",
                statusArrived,
                R.mipmap.ic_launcher
        ));

        if (statusFilter.equals(getString(R.string.sponsor_history_tab_all))) {
            return pageItems;
        }

        for (SponsorHistoryAdapter.HistoryItem item : pageItems) {
            if (item.status.equals(statusFilter)) {
                items.add(item);
            }
        }
        return items;
    }
}
