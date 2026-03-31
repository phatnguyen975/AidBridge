package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.view.View;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerHistoryBinding;
import com.drc.aidbridge.ui.main.adapter.volunteer.VolunteerMissionHistoryAdapter;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class VolunteerHistoryFragment extends BaseFragment<FragmentVolunteerHistoryBinding> {

    private static final String TAG = "VolunteerHistory";

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private String currentFilter = "";

    @Inject
    VolunteerMissionHistoryAdapter volunteerMissionHistoryAdapter;

    @Override
    protected FragmentVolunteerHistoryBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentVolunteerHistoryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        onInitUI();
        setupToolbar();
        setupFilterLogging();
    }

    @Override
    protected void observeViewModel() {
        // TODO: Inject ViewModel và observe LiveData cho danh sách lịch sử từ UseCase
    }

    private void onInitUI() {
        resetScreenState();
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(volunteerMissionHistoryAdapter);
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

    private void setupFilterLogging() {
        binding.chipGroupMissionFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                Log.d(TAG, "Filter selected: none");
                return;
            }

            int selectedId = checkedIds.get(0);
            Chip selectedChip = group.findViewById(selectedId);
            String selectedText = selectedChip != null ? selectedChip.getText().toString() : "unknown";
            Log.d(TAG, "Filter selected: " + selectedText + " (id=" + selectedId + ")");

            volunteerMissionHistoryAdapter.clear();
            currentPage = 1;
            isLastPage = false;
            currentFilter = selectedText;
            loadData();

            // TODO: Gọi viewModel.fetchHistory(type) và observe dữ liệu để cập nhật vào
            // volunteerMissionHistoryAdapter.setMissionList()
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
}
