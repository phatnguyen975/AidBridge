package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerHistoryBinding;
import com.drc.aidbridge.domain.model.volunteer.VolunteerHistoryItem;
import com.drc.aidbridge.ui.main.adapter.volunteer.VolunteerMissionHistoryAdapter;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerHistoryViewModel;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class VolunteerHistoryFragment extends BaseFragment<FragmentVolunteerHistoryBinding> {

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private String currentFilter = "";

    private static final String FILTER_ALL = "all";
    private static final String FILTER_RESCUE = "RESCUE";
    private static final String FILTER_DELIVERY = "DELIVERY";

    private VolunteerHistoryViewModel volunteerHistoryViewModel;

    @Inject
    VolunteerMissionHistoryAdapter volunteerMissionHistoryAdapter;

    @Override
    protected FragmentVolunteerHistoryBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentVolunteerHistoryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        volunteerHistoryViewModel = new ViewModelProvider(this).get(VolunteerHistoryViewModel.class);
        onInitUI();
        setupToolbar();
        setupFilterActions();

        // === API LOGIC (Uncomment when Backend is ready) ===
        // volunteerHistoryViewModel.loadHistory();
        // ===================================================
    }

    @Override
    protected void observeViewModel() {
        if (volunteerHistoryViewModel == null) {
            return;
        }

        volunteerHistoryViewModel.getHistoryResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) {
                return;
            }

            binding.paginationProgress.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);

            if (result.hasBeenHandled() && !result.isLoading()) {
                return;
            }

            if (result.isError()) {
                result.markAsHandled();
                String message = result.getMessage() != null
                        ? result.getMessage()
                        : getString(R.string.error_generic);
                showTopSnackbar(binding.getRoot(), message, true);
            }
        });

        volunteerHistoryViewModel.getFilteredHistoryList().observe(getViewLifecycleOwner(), historyItems -> {
            // Mock mode: keep adapter data from loadData()/generateDummyData().
            // List<VolunteerMissionHistoryAdapter.MissionHistoryModel> uiModels =
            // mapToUiModels(historyItems);
            // volunteerMissionHistoryAdapter.setMissionList(uiModels);
        });
    }

    private void onInitUI() {
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(volunteerMissionHistoryAdapter);

        resetScreenState();
        setupRecyclerViewScrollListener();
        currentFilter = getString(R.string.volunteer_history_filter_all);
        loadData();
    }

    private void resetScreenState() {
        currentPage = 1;
        isLoading = false;
        isLastPage = false;
        currentFilter = getString(R.string.volunteer_history_filter_all);
    }

    private void setupRecyclerViewScrollListener() {
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
                    loadData();
                }
            }
        });
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> popBackStackSafely());
    }

    private void setupFilterActions() {
        binding.chipGroupMissionFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                return;
            }

            int selectedId = checkedIds.get(0);
            Chip selectedChip = group.findViewById(selectedId);
            String selectedText = selectedChip != null
                    ? selectedChip.getText().toString()
                    : getString(R.string.volunteer_history_filter_all);

            volunteerMissionHistoryAdapter.clear();
            currentPage = 1;
            isLastPage = false;
            currentFilter = selectedText;
            loadData();

            // === API LOGIC (Uncomment when Backend is ready) ===
            // if (selectedId == R.id.chipFilterRescue) {
            // volunteerHistoryViewModel.filterHistory(FILTER_RESCUE);
            // } else if (selectedId == R.id.chipFilterDelivery) {
            // volunteerHistoryViewModel.filterHistory(FILTER_DELIVERY);
            // } else {
            // volunteerHistoryViewModel.filterHistory(FILTER_ALL);
            // }
            // ===================================================
        });
    }

    private void loadData() {
        if (isLoading || isLastPage) {
            return;
        }

        isLoading = true;
        binding.paginationProgress.setVisibility(View.VISIBLE);

        List<VolunteerMissionHistoryAdapter.MissionHistoryModel> pageData = generateDummyData(currentPage,
                currentFilter);
        if (pageData.isEmpty()) {
            isLastPage = true;
        } else {
            volunteerMissionHistoryAdapter.addItems(pageData);
            if (currentPage >= 4) {
                isLastPage = true;
            }
        }

        isLoading = false;
        binding.paginationProgress.setVisibility(View.GONE);
    }

    private List<VolunteerMissionHistoryAdapter.MissionHistoryModel> generateDummyData(int page,
            @NonNull String filter) {
        List<VolunteerMissionHistoryAdapter.MissionHistoryModel> allItems = new ArrayList<>();

        String typeRescue = getString(R.string.volunteer_mission_type_rescue);
        String typeDelivery = getString(R.string.volunteer_mission_type_delivery);

        allItems.add(new VolunteerMissionHistoryAdapter.MissionHistoryModel(
                typeRescue,
                getString(R.string.volunteer_mission_dummy_location_1),
                getString(R.string.volunteer_mission_dummy_time_1, page),
                "https://images.unsplash.com/photo-1523482580672-f109ba8cb9be"));

        allItems.add(new VolunteerMissionHistoryAdapter.MissionHistoryModel(
                typeDelivery,
                getString(R.string.volunteer_mission_dummy_location_2),
                getString(R.string.volunteer_mission_dummy_time_2, page),
                "https://images.unsplash.com/photo-1469474968028-56623f02e42e"));

        allItems.add(new VolunteerMissionHistoryAdapter.MissionHistoryModel(
                typeRescue,
                getString(R.string.volunteer_mission_dummy_location_3),
                getString(R.string.volunteer_mission_dummy_time_3, page),
                "https://images.unsplash.com/photo-1506744038136-46273834b3fb"));

        allItems.add(new VolunteerMissionHistoryAdapter.MissionHistoryModel(
                typeDelivery,
                getString(R.string.volunteer_mission_dummy_location_4),
                getString(R.string.volunteer_mission_dummy_time_4, page),
                "https://images.unsplash.com/photo-1509099836639-18ba1795216d"));

        allItems.add(new VolunteerMissionHistoryAdapter.MissionHistoryModel(
                typeRescue,
                getString(R.string.volunteer_mission_dummy_location_5),
                getString(R.string.volunteer_mission_dummy_time_5, page),
                "https://images.unsplash.com/photo-1618477202872-4984f4bf49ea"));

        allItems.add(new VolunteerMissionHistoryAdapter.MissionHistoryModel(
                typeDelivery,
                getString(R.string.volunteer_mission_dummy_location_6),
                getString(R.string.volunteer_mission_dummy_time_6, page),
                "https://images.unsplash.com/photo-1446776877081-d282a0f896e2"));

        if (filter.equalsIgnoreCase(getString(R.string.volunteer_history_filter_all))) {
            return allItems;
        }

        List<VolunteerMissionHistoryAdapter.MissionHistoryModel> filteredItems = new ArrayList<>();
        for (VolunteerMissionHistoryAdapter.MissionHistoryModel item : allItems) {
            if (filter.equalsIgnoreCase(getString(R.string.volunteer_history_filter_rescue))
                    && item.getType().equalsIgnoreCase(typeRescue)) {
                filteredItems.add(item);
            } else if (filter.equalsIgnoreCase(getString(R.string.volunteer_history_filter_delivery))
                    && item.getType().equalsIgnoreCase(typeDelivery)) {
                filteredItems.add(item);
            }
        }

        return filteredItems;
    }

    private List<VolunteerMissionHistoryAdapter.MissionHistoryModel> mapToUiModels(
            @Nullable List<VolunteerHistoryItem> historyItems) {
        List<VolunteerMissionHistoryAdapter.MissionHistoryModel> allItems = new ArrayList<>();

        if (historyItems == null) {
            return allItems;
        }

        for (VolunteerHistoryItem item : historyItems) {
            String type = item.getType() != null ? item.getType() : "";
            String location = item.getLocation() != null ? item.getLocation() : "";
            String completedAt = item.getCompletedAt() != null ? item.getCompletedAt() : "";

            allItems.add(new VolunteerMissionHistoryAdapter.MissionHistoryModel(
                    type,
                    location,
                    completedAt,
                    "https://images.unsplash.com/photo-1469474968028-56623f02e42e"));
        }

        return allItems;
    }
}
