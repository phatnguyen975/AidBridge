package com.drc.aidbridge.ui.main.fragment.staff;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentStaffTasksBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.staff.StaffTaskAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffTasksFragment extends BaseFragment<FragmentStaffTasksBinding> {

    private static final int TAB_EXPORT = 0;
    private static final int TAB_IMPORT = 1;
    private static final int MAX_PAGE = 5;
    private static final int ITEMS_PER_PAGE = 6;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private StaffTaskAdapter adapter;
    private int currentTab = TAB_EXPORT;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Nullable
    @Override
    protected FragmentStaffTasksBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffTasksBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        setupTabs();
        setupRecycler();
        setupPagination();
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
                currentPage = 1;
                isLastPage = false;
                isLoading = false;
                binding.progressInitial.setVisibility(android.view.View.GONE);
                adapter.setLoadingMore(false);
                adapter.clear();
                loadMockData();
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
        adapter = new StaffTaskAdapter(getChildFragmentManager());
        binding.rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTasks.setAdapter(adapter);
    }

    private void setupPagination() {
        binding.rvTasks.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

    private void loadMockData() {
        if (isLoading || isLastPage) {
            return;
        }

        isLoading = true;
        boolean isInitialLoading = adapter.getDataCount() == 0;
        binding.progressInitial.setVisibility(
            isInitialLoading ? android.view.View.VISIBLE : android.view.View.GONE
        );
        adapter.setLoadingMore(!isInitialLoading);

        handler.postDelayed(() -> {
            List<StaffTaskAdapter.TaskItem> newItems = buildMockTasks(currentTab, currentPage);
            if (newItems.isEmpty()) {
                isLastPage = true;
            } else {
                adapter.addItems(newItems);
                if (currentPage >= MAX_PAGE) {
                    isLastPage = true;
                }
            }

            isLoading = false;
            if (binding != null) {
                binding.progressInitial.setVisibility(android.view.View.GONE);
                adapter.setLoadingMore(false);
            }
        }, 500L);
    }

    private List<StaffTaskAdapter.TaskItem> buildMockTasks(int tab, int page) {
        if (page > MAX_PAGE) {
            return new ArrayList<>();
        }

        List<StaffTaskAdapter.TaskItem> items = new ArrayList<>();
        int base = (page - 1) * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int sequence = base + i + 1;
            boolean isIncoming = sequence % 3 != 0;
            String status = isIncoming
                ? getString(R.string.staff_task_status_incoming)
                : getString(R.string.staff_task_status_completed);
            String type = tab == TAB_EXPORT
                ? StaffTaskAdapter.TYPE_EXPORT
                : StaffTaskAdapter.TYPE_IMPORT;

            items.add(new StaffTaskAdapter.TaskItem(
                "task_" + tab + "_" + page + "_" + i,
                type,
                getString(R.string.staff_task_eta_format, 10 + (sequence * 5)),
                status,
                buildTaskCode(type, sequence),
                buildPersonName(type, sequence),
                getString(R.string.staff_task_phone_format, sequence),
                buildExpectedItems(type, sequence)
            ));
        }
        return items;
    }

    private String buildTaskCode(String type, int sequence) {
        if (StaffTaskAdapter.TYPE_EXPORT.equals(type)) {
            return getString(R.string.staff_task_code_export_format, 12000 + sequence);
        }
        return getString(R.string.staff_task_code_import_format, 22000 + sequence);
    }

    private String buildPersonName(String type, int sequence) {
        if (StaffTaskAdapter.TYPE_EXPORT.equals(type)) {
            return getString(R.string.staff_task_person_export_format, sequence);
        }
        return getString(R.string.staff_task_person_import_format, sequence);
    }

    private ArrayList<String> buildExpectedItems(String type, int sequence) {
        ArrayList<String> list = new ArrayList<>();

        if (StaffTaskAdapter.TYPE_EXPORT.equals(type)) {
            list.add(getString(
                R.string.staff_task_item_summary_format,
                getString(R.string.staff_detail_item_water),
                8 + sequence,
                getString(R.string.staff_detail_unit_box)
            ));
            list.add(getString(
                R.string.staff_task_item_summary_format,
                getString(R.string.staff_detail_item_noodle),
                6 + sequence,
                getString(R.string.staff_detail_unit_box)
            ));
        } else {
            list.add(getString(
                R.string.staff_task_item_summary_format,
                getString(R.string.staff_detail_item_milk),
                4 + sequence,
                getString(R.string.staff_detail_unit_box)
            ));
            list.add(getString(
                R.string.staff_task_item_summary_format,
                getString(R.string.staff_detail_item_rescue_blanket_format, sequence),
                3 + sequence,
                getString(R.string.staff_detail_unit_bundle)
            ));
        }

        return list;
    }
}
