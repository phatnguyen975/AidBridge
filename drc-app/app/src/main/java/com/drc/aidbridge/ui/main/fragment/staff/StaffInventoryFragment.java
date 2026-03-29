package com.drc.aidbridge.ui.main.fragment.staff;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentStaffInventoryBinding;
import com.drc.aidbridge.ui.main.adapter.staff.StaffInventoryAdapter;
import com.drc.aidbridge.ui.base.BaseFragment;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffInventoryFragment extends BaseFragment<FragmentStaffInventoryBinding> {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private StaffInventoryAdapter adapter;
    private int baseRecyclerBottomPadding;

    private int currentPage = 1;
    private String currentCategoryCode = "ALL";
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Nullable
    @Override
    protected FragmentStaffInventoryBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffInventoryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        setupRecyclerView();
        setupPagination();
        setupFilter();
        setupActionButtons();
        loadMockData();
    }

    @Override
    protected void observeViewModel() {
    }

    private void setupRecyclerView() {
        adapter = new StaffInventoryAdapter();
        binding.rvInventory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvInventory.setAdapter(adapter);
        baseRecyclerBottomPadding = binding.rvInventory.getPaddingBottom();
    }

    private void setupFilter() {
        binding.chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                binding.chipAll.setChecked(true);
                return;
            }
            currentCategoryCode = mapChipIdToCategoryCode(checkedIds.get(0));
            currentPage = 1;
            isLastPage = false;
            adapter.clear();
            loadMockData();
        });
    }

    private void setupActionButtons() {
        binding.btnStockIn.setOnClickListener(v -> navigateToScanner("import"));
        binding.btnStockOut.setOnClickListener(v -> navigateToScanner("export"));
    }

    private void navigateToScanner(String mode) {
        Bundle bundle = new Bundle();
        bundle.putString("mode", mode);
        navigateSafely(R.id.action_staffInventoryFragment_to_staffScannerFragment, bundle);
    }

    private void setupPagination() {
        binding.rvInventory.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
                if (total == 0) {
                    return;
                }

                if (lastVisible >= total - 2) {
                    currentPage++;
                    loadMockData();
                }
            }
        });
    }

    private String mapChipIdToCategoryCode(int checkedId) {
        if (checkedId == R.id.chip_food) {
            return "FOOD";
        }
        if (checkedId == R.id.chip_water) {
            return "WATER";
        }
        if (checkedId == R.id.chip_medical) {
            return "MEDICAL";
        }
        return "ALL";
    }

    private void loadMockData() {
        if (isLoading || isLastPage) {
            return;
        }

        isLoading = true;
        updatePaginationLoading(true);

        handler.postDelayed(() -> {
            List<StaffInventoryAdapter.InventoryItem> items = buildMockItems(currentCategoryCode, currentPage);
            if (items.isEmpty()) {
                isLastPage = true;
            } else {
                adapter.addItems(items);
                if (currentPage >= 3) {
                    isLastPage = true;
                }
            }

            isLoading = false;
            if (binding != null) {
                updatePaginationLoading(false);
            }
        }, 500L);
    }

    private void updatePaginationLoading(boolean show) {
        boolean hasLoadedItems = adapter != null && adapter.getItemCount() > 0;

        if (!show) {
            binding.initialLoadingProgress.setVisibility(android.view.View.GONE);
            binding.paginationProgress.setVisibility(android.view.View.GONE);
            setTemporaryBottomSpace(false);
            return;
        }

        binding.initialLoadingProgress.setVisibility(hasLoadedItems ? android.view.View.GONE : android.view.View.VISIBLE);
        binding.paginationProgress.setVisibility(hasLoadedItems ? android.view.View.VISIBLE : android.view.View.GONE);
        setTemporaryBottomSpace(hasLoadedItems);
    }

    private void setTemporaryBottomSpace(boolean enabled) {
        int extraSpace = enabled ? getResources().getDimensionPixelSize(R.dimen.spacing_xxl) : 0;
        binding.rvInventory.setPaddingRelative(
                binding.rvInventory.getPaddingStart(),
                binding.rvInventory.getPaddingTop(),
                binding.rvInventory.getPaddingEnd(),
                baseRecyclerBottomPadding + extraSpace
        );
    }

    private List<StaffInventoryAdapter.InventoryItem> buildMockItems(String categoryCode, int page) {
        List<StaffInventoryAdapter.InventoryItem> source = createBaseItems();
        List<StaffInventoryAdapter.InventoryItem> filtered = new ArrayList<>();

        for (StaffInventoryAdapter.InventoryItem item : source) {
            if ("ALL".equals(categoryCode) || item.categoryName.equals(resolveCategoryLabel(categoryCode))) {
                String generatedId = item.id + "-p" + page;
                int adjustedQuantity = Math.max(1, item.quantity - ((page - 1) * 2));
                filtered.add(new StaffInventoryAdapter.InventoryItem(
                        generatedId,
                        item.name,
                        item.categoryName,
                        item.status,
                        adjustedQuantity,
                        item.unit,
                        item.leftBorderColorRes
                ));
            }
        }

        return filtered;
    }

    private List<StaffInventoryAdapter.InventoryItem> createBaseItems() {
        List<StaffInventoryAdapter.InventoryItem> items = new ArrayList<>();

        items.add(new StaffInventoryAdapter.InventoryItem(
                "1",
                "Mì gói Hảo Hảo",
                getString(R.string.staff_inventory_chip_food),
                getString(R.string.staff_inventory_status_shortage),
                12,
                "hộp",
                R.color.sos_red
        ));
        items.add(new StaffInventoryAdapter.InventoryItem(
                "2",
                "Nước suối 500ml",
                getString(R.string.staff_inventory_chip_water),
                getString(R.string.staff_inventory_status_low_stock),
                24,
                "thùng",
                R.color.warning_orange
        ));
        items.add(new StaffInventoryAdapter.InventoryItem(
                "3",
                "Băng gạc y tế",
                getString(R.string.staff_inventory_chip_medical),
                getString(R.string.staff_inventory_status_stable),
                48,
                "hộp",
                R.color.safe_green
        ));
        items.add(new StaffInventoryAdapter.InventoryItem(
                "4",
                "Gạo cứu trợ",
                getString(R.string.staff_inventory_chip_food),
                getString(R.string.staff_inventory_status_stable),
                36,
                "bao",
                R.color.safe_green
        ));
        items.add(new StaffInventoryAdapter.InventoryItem(
                "5",
                "Nước điện giải",
                getString(R.string.staff_inventory_chip_water),
                getString(R.string.staff_inventory_status_shortage),
                8,
                "thùng",
                R.color.sos_red
        ));
        items.add(new StaffInventoryAdapter.InventoryItem(
                "6",
                "Thuốc hạ sốt",
                getString(R.string.staff_inventory_chip_medical),
                getString(R.string.staff_inventory_status_low_stock),
                16,
                "vỉ",
                R.color.warning_orange
        ));
        return items;
    }

    private String resolveCategoryLabel(String categoryCode) {
        if ("FOOD".equals(categoryCode)) {
            return getString(R.string.staff_inventory_chip_food);
        }
        if ("WATER".equals(categoryCode)) {
            return getString(R.string.staff_inventory_chip_water);
        }
        if ("MEDICAL".equals(categoryCode)) {
            return getString(R.string.staff_inventory_chip_medical);
        }
        return getString(R.string.staff_inventory_chip_all);
    }
}
