package com.drc.aidbridge.ui.main.fragment.staff;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.FragmentStaffTasksBinding;
import com.drc.aidbridge.domain.model.staff.StaffUpcomingTask;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.staff.StaffTaskAdapter;
import com.drc.aidbridge.ui.main.viewmodel.staff.StaffTasksViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.Collections;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffTasksFragment extends BaseFragment<FragmentStaffTasksBinding> {

    private static final int TAB_EXPORT = 0;
    private static final int TAB_IMPORT = 1;

    private StaffTaskAdapter adapter;
    private int currentTab = TAB_EXPORT;
    private StaffTasksViewModel viewModel;

    @Nullable
    @Override
    protected FragmentStaffTasksBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffTasksBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(StaffTasksViewModel.class);
        setupTabs();
        setupRecycler();
        loadTasksByTab();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getTasksResult().observe(getViewLifecycleOwner(), this::renderTasksResult);
    }

    private void setupTabs() {
        setupTabViews();
        binding.tabTasks.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int selectedPosition = tab.getPosition();
                updateTabTextState(selectedPosition);
                if (selectedPosition == currentTab) {
                    return;
                }

                currentTab = selectedPosition;
                adapter.clear();
                loadTasksByTab();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                updateTabTextState(binding.tabTasks.getSelectedTabPosition());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        updateTabTextState(currentTab);
    }

    private void setupTabViews() {
        setTabCustomView(0, R.string.staff_tasks_tab_export);
        setTabCustomView(1, R.string.staff_tasks_tab_import);
    }

    private void setTabCustomView(int index, int textRes) {
        TabLayout.Tab tab = binding.tabTasks.getTabAt(index);
        if (tab == null) {
            return;
        }
        TextView tabView = (TextView) LayoutInflater.from(requireContext())
            .inflate(R.layout.item_staff_tasks_tab, binding.tabTasks, false);
        tabView.setText(textRes);
        tab.setCustomView(tabView);
    }

    private void updateTabTextState(int selectedIndex) {
        for (int i = 0; i < binding.tabTasks.getTabCount(); i++) {
            TabLayout.Tab tab = binding.tabTasks.getTabAt(i);
            if (tab == null || !(tab.getCustomView() instanceof TextView)) {
                continue;
            }
            TextView textView = (TextView) tab.getCustomView();
            int colorRes = i == selectedIndex ? R.color.text_primary : R.color.text_secondary;
            textView.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
        }
    }

    private void setupRecycler() {
        adapter = new StaffTaskAdapter();
        binding.rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTasks.setAdapter(adapter);
    }

    private void loadTasksByTab() {
        if (currentTab == TAB_EXPORT) {
            viewModel.loadDeliveries();
            return;
        }
        viewModel.loadDonations();
    }

    private void renderTasksResult(@Nullable NetworkResultWrapper<List<StaffUpcomingTask>> result) {
        if (binding == null) {
            return;
        }

        if (result == null) {
            renderError(getString(R.string.error_generic));
            return;
        }

        if (result.isLoading()) {
            binding.progressInitial.setVisibility(View.VISIBLE);
            return;
        }

        binding.progressInitial.setVisibility(View.GONE);

        if (result.isError()) {
            renderError(toFriendlyUiError(result.getMessage()));
            return;
        }

        List<StaffUpcomingTask> tasks = result.getData() != null
                ? result.getData()
                : Collections.emptyList();
        adapter.clear();
        adapter.addItems(tasks);
    }

    private void renderError(@Nullable String message) {
        binding.progressInitial.setVisibility(View.GONE);
        adapter.clear();
        showTopSnackbar(binding.getRoot(), message != null ? message : getString(R.string.error_generic), true);
    }

    private String toFriendlyUiError(@Nullable String rawMessage) {
        String message = rawMessage != null ? rawMessage.trim() : "";
        return message.isEmpty() ? getString(R.string.error_generic) : message;
    }
}
