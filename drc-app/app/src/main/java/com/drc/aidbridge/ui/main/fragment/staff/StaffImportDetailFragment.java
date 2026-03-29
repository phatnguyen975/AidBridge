package com.drc.aidbridge.ui.main.fragment.staff;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentStaffImportDetailBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.staff.StaffImportDetailAdapter;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffImportDetailFragment extends BaseFragment<FragmentStaffImportDetailBinding> {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private StaffImportDetailAdapter adapter;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Nullable
    @Override
    protected FragmentStaffImportDetailBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffImportDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        setupToolbar();
        setupRecycler();
        setupPagination();
        setupActions();
        loadMockData();
    }

    @Override
    protected void observeViewModel() {
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }

    private void setupToolbar() {
        binding.ivBack.setOnClickListener(v -> popBackStackSafely());
    }

    private void setupRecycler() {
        adapter = new StaffImportDetailAdapter();
        binding.rvItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvItems.setAdapter(adapter);
    }

    private void setupPagination() {
        binding.rvItems.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0 || isLoading || isLastPage) {
                    return;
                }
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (!(layoutManager instanceof LinearLayoutManager)) {
                    return;
                }
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                int total = linearLayoutManager.getItemCount();
                int lastVisible = linearLayoutManager.findLastVisibleItemPosition();
                if (total > 0 && lastVisible >= total - 1) {
                    currentPage++;
                    loadMockData();
                }
            }
        });
    }

    private void setupActions() {
        binding.btnConfirm.setOnClickListener(v -> popBackStackSafely(R.id.staffInventoryFragment, false));
    }

    private void loadMockData() {
        if (isLoading || isLastPage) {
            return;
        }

        isLoading = true;
        boolean isInitialLoad = adapter.getDataCount() == 0;
        binding.progressPagination.setVisibility(
            isInitialLoad ? android.view.View.VISIBLE : android.view.View.GONE
        );
        adapter.setLoadingMore(!isInitialLoad);

        handler.postDelayed(() -> {
            List<StaffImportDetailAdapter.ImportDetailItem> newItems = buildMockItems(currentPage);
            if (newItems.isEmpty()) {
                isLastPage = true;
            } else {
                adapter.addItems(newItems);
                if (currentPage >= 3) {
                    isLastPage = true;
                }
            }

            isLoading = false;
            if (binding != null) {
                adapter.setLoadingMore(false);
                binding.progressPagination.setVisibility(android.view.View.GONE);
            }
        }, 500L);
    }

    private List<StaffImportDetailAdapter.ImportDetailItem> buildMockItems(int page) {
        if (page > 3) {
            return new ArrayList<>();
        }

        List<StaffImportDetailAdapter.ImportDetailItem> items = new ArrayList<>();
        if (page == 1) {
            items.add(new StaffImportDetailAdapter.ImportDetailItem(
                getString(R.string.staff_detail_item_noodle),
                10,
                getString(R.string.staff_detail_unit_box)
            ));
            items.add(new StaffImportDetailAdapter.ImportDetailItem(
                getString(R.string.staff_detail_item_water),
                12,
                getString(R.string.staff_detail_unit_box)
            ));
            items.add(new StaffImportDetailAdapter.ImportDetailItem(
                getString(R.string.staff_detail_item_milk),
                8,
                getString(R.string.staff_detail_unit_box)
            ));
            return items;
        }

        int base = page * 4;
        items.add(new StaffImportDetailAdapter.ImportDetailItem(
            getString(R.string.staff_detail_item_medical_format, page),
            6 + base,
            getString(R.string.staff_detail_unit_pack)
        ));
        items.add(new StaffImportDetailAdapter.ImportDetailItem(
            getString(R.string.staff_detail_item_nutrition_format, page),
            4 + base,
            getString(R.string.staff_detail_unit_box)
        ));
        items.add(new StaffImportDetailAdapter.ImportDetailItem(
            getString(R.string.staff_detail_item_rescue_blanket_format, page),
            3 + base,
            getString(R.string.staff_detail_unit_bundle)
        ));
        return items;
    }
}
