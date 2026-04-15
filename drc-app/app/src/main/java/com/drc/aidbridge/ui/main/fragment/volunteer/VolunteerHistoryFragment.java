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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class VolunteerHistoryFragment extends BaseFragment<FragmentVolunteerHistoryBinding> {

    private static final DateTimeFormatter HISTORY_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
            Locale.getDefault());

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
        volunteerHistoryViewModel.loadHistory();
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
            List<VolunteerMissionHistoryAdapter.MissionHistoryModel> uiModels = mapToUiModels(historyItems);
            volunteerMissionHistoryAdapter.setMissionList(uiModels);
        });
    }

    private void onInitUI() {
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(volunteerMissionHistoryAdapter);
        setupRecyclerViewScrollListener();
    }

    private void setupRecyclerViewScrollListener() {
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
                    volunteerHistoryViewModel.loadNextPage();
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
            // if (selectedId == R.id.chipFilterRescue) {
            // volunteerHistoryViewModel.filterHistory(FILTER_RESCUE);
            // }

            if (selectedId == R.id.chipFilterRescue) {
                volunteerHistoryViewModel.filterHistory(FILTER_RESCUE);
            } else if (selectedId == R.id.chipFilterDelivery) {
                volunteerHistoryViewModel.filterHistory(FILTER_DELIVERY);
            } else {
                volunteerHistoryViewModel.filterHistory(FILTER_ALL);
            }
        });
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
            String completedAt = formatCompletedAt(item.getCompletedAt());

            allItems.add(new VolunteerMissionHistoryAdapter.MissionHistoryModel(
                    type,
                    location,
                    completedAt,
                    "https://img.cand.com.vn/resize/800x800/NewFiles/Images/2024/09/09/di_doi_nguoi_va_tai_san_ra_khoi_-1725881783067.JPG"));
        }

        return allItems;
    }

    private String formatCompletedAt(@Nullable String rawCompletedAt) {
        if (rawCompletedAt == null || rawCompletedAt.trim().isEmpty()) {
            return "";
        }

        String normalized = rawCompletedAt.trim();
        try {
            // ISO-8601 from backend (e.g. 2026-04-15T10:12:34Z)
            Instant instant = Instant.parse(normalized);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(HISTORY_TIME_FORMATTER);
        } catch (Exception ignored) {
            // Continue to epoch parsing fallback.
        }

        try {
            // Support epoch seconds / milliseconds payloads.
            long epoch = Long.parseLong(normalized);
            if (normalized.length() <= 10) {
                epoch = epoch * 1000L;
            }
            Instant instant = Instant.ofEpochMilli(epoch);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(HISTORY_TIME_FORMATTER);
        } catch (Exception ignored) {
            // Last resort: keep original text to avoid blank data.
        }

        return normalized;
    }
}
