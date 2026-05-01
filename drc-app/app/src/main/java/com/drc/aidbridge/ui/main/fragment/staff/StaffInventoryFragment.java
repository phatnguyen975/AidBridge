package com.drc.aidbridge.ui.main.fragment.staff;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.FragmentStaffInventoryBinding;
import com.drc.aidbridge.domain.model.staff.StaffInventory;
import com.drc.aidbridge.domain.model.staff.StaffInventoryFilter;
import com.drc.aidbridge.domain.model.staff.StaffInventoryHub;
import com.drc.aidbridge.domain.model.staff.StaffInventoryItem;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.staff.StaffInventoryAdapter;
import com.drc.aidbridge.ui.main.viewmodel.staff.StaffInventoryViewModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffInventoryFragment extends BaseFragment<FragmentStaffInventoryBinding> {

    private static final long SEARCH_DEBOUNCE_MS = 400L;
    private static final String CATEGORY_WATER = "N\u01b0\u1edbc u\u1ed1ng";
    private static final String CATEGORY_NECESSITIES = "Nhu y\u1ebfu ph\u1ea9m kh\u00e1c";
    private static final String CATEGORY_CLOTHING = "Qu\u1ea7n \u00e1o";
    private static final String CATEGORY_MEDICINE = "Thu\u1ed1c";
    private static final String CATEGORY_FOOD = "Th\u1ee9c \u0103n";

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final Map<String, String> filterIdByName = new HashMap<>();

    private StaffInventoryViewModel viewModel;
    private StaffInventoryAdapter adapter;
    private Runnable pendingSearchRunnable;

    @Nullable
    @Override
    protected FragmentStaffInventoryBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffInventoryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(StaffInventoryViewModel.class);
        setupRecyclerView();
        setupFilter();
        setupSearch();
        setupActionButtons();
        setupRetry();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getInventoryResult().observe(getViewLifecycleOwner(), this::renderInventoryResult);
    }

    @Override
    public void onDestroyView() {
        searchHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private void setupRecyclerView() {
        adapter = new StaffInventoryAdapter();
        binding.rvInventory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvInventory.setAdapter(adapter);
    }

    private void setupFilter() {
        binding.chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                binding.chipAll.setChecked(true);
                return;
            }

            String parentName = mapChipIdToParentName(checkedIds.get(0));
            String parentId = parentName != null ? filterIdByName.get(parentName) : null;
            viewModel.selectFilter(parentId, parentName);
        });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pendingSearchRunnable != null) {
                    searchHandler.removeCallbacks(pendingSearchRunnable);
                }
                String keyword = s != null ? s.toString() : "";
                pendingSearchRunnable = () -> viewModel.updateKeyword(keyword);
                searchHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupActionButtons() {
        binding.btnStockIn.setOnClickListener(v -> navigateToScanner("import"));
        binding.btnStockOut.setOnClickListener(v -> navigateToScanner("export"));
    }

    private void setupRetry() {
        binding.btnRetry.setOnClickListener(v -> viewModel.refresh());
    }

    private void navigateToScanner(String mode) {
        Bundle bundle = new Bundle();
        bundle.putString("mode", mode);
        navigateSafely(R.id.action_staffInventoryFragment_to_staffScannerFragment, bundle);
    }

    private void renderInventoryResult(@Nullable NetworkResultWrapper<StaffInventory> result) {
        if (result == null) {
            renderError(getString(R.string.staff_inventory_error_generic));
            return;
        }

        if (result.isLoading()) {
            renderLoading();
            return;
        }

        if (result.isError()) {
            renderError(toFriendlyUiError(result.getMessage()));
            return;
        }

        renderInventory(result.getData());
    }

    private void renderLoading() {
        binding.initialLoadingProgress.setVisibility(View.VISIBLE);
        binding.paginationProgress.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.GONE);
        binding.layoutErrorState.setVisibility(View.GONE);
    }

    private void renderInventory(@Nullable StaffInventory inventory) {
        binding.initialLoadingProgress.setVisibility(View.GONE);
        binding.paginationProgress.setVisibility(View.GONE);
        binding.layoutErrorState.setVisibility(View.GONE);

        StaffInventoryHub hub = inventory != null ? inventory.getHub() : null;
        String hubName = hub != null && !hub.getName().isEmpty()
                ? hub.getName()
                : getString(R.string.staff_inventory_hub_unknown);
        binding.tvHubName.setText(getString(R.string.staff_inventory_hub_format, hubName));

        renderFilters(inventory != null ? inventory.getFilters() : Collections.emptyList());

        List<StaffInventoryItem> items = inventory != null
                ? inventory.getItems()
                : Collections.emptyList();
        adapter.submitList(items);

        boolean isEmpty = items.isEmpty();
        binding.rvInventory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void renderFilters(@NonNull List<StaffInventoryFilter> filters) {
        filterIdByName.clear();
        for (StaffInventoryFilter filter : filters) {
            if (filter == null || filter.isAll()) {
                continue;
            }
            filterIdByName.put(filter.getName(), filter.getId());
        }
    }

    private void renderError(@NonNull String message) {
        binding.initialLoadingProgress.setVisibility(View.GONE);
        binding.paginationProgress.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.GONE);
        binding.rvInventory.setVisibility(View.GONE);
        binding.layoutErrorState.setVisibility(View.VISIBLE);
        binding.tvErrorMessage.setText(message);
        adapter.clear();
    }

    @Nullable
    private String mapChipIdToParentName(int checkedId) {
        if (checkedId == R.id.chip_water) {
            return CATEGORY_WATER;
        }
        if (checkedId == R.id.chip_necessities) {
            return CATEGORY_NECESSITIES;
        }
        if (checkedId == R.id.chip_clothing) {
            return CATEGORY_CLOTHING;
        }
        if (checkedId == R.id.chip_medicine) {
            return CATEGORY_MEDICINE;
        }
        if (checkedId == R.id.chip_food) {
            return CATEGORY_FOOD;
        }
        return null;
    }

    private String toFriendlyUiError(@Nullable String rawMessage) {
        String message = rawMessage != null ? rawMessage.trim() : "";
        String lower = message.toLowerCase();
        if (message.isEmpty()
                || lower.contains("jdbc exception")
                || lower.contains("sql")
                || lower.contains("select ")
                || lower.contains("lower(")
                || lower.contains("org.springframework")) {
            return getString(R.string.staff_inventory_error_generic);
        }
        return message;
    }
}
