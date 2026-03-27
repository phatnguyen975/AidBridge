package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerHistoryBinding;
import com.drc.aidbridge.databinding.ItemVolunteerMissionHistoryBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerHistoryFragment extends BaseFragment<FragmentVolunteerHistoryBinding> {

    private static final String TAG = "VolunteerHistory";
    private static final String TYPE_RESCUE = "rescue";
    private static final String TYPE_DELIVERY = "delivery";

    @Override
    protected FragmentVolunteerHistoryBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentVolunteerHistoryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        setupToolbar();
        setupRecyclerView();
        setupFilterLogging();
    }

    @Override
    protected void observeViewModel() {
        // TODO: Inject ViewModel và observe LiveData cho danh sách lịch sử từ UseCase
    }

    private void setupToolbar() {
        binding.toolbarVolunteerHistory.setNavigationOnClickListener(v -> popBackStackSafely());
    }

    private void setupRecyclerView() {
        binding.rvVolunteerMissionHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvVolunteerMissionHistory.setAdapter(new MissionHistoryAdapter(createDemoData()));
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
        });
    }

    private List<MissionHistory> createDemoData() {
        List<MissionHistory> missions = new ArrayList<>();

        missions.add(new MissionHistory(
                TYPE_RESCUE,
                getString(R.string.volunteer_mission_history_location_placeholder),
                getString(R.string.volunteer_mission_history_datetime_placeholder),
                "https://images.unsplash.com/photo-1523482580672-f109ba8cb9be"));

        missions.add(new MissionHistory(
                TYPE_DELIVERY,
                getString(R.string.volunteer_mission_history_location_placeholder_second),
                getString(R.string.volunteer_mission_history_datetime_placeholder_second),
                "https://images.unsplash.com/photo-1469474968028-56623f02e42e"));

        missions.add(new MissionHistory(
                TYPE_RESCUE,
                getString(R.string.volunteer_mission_history_location_placeholder_third),
                getString(R.string.volunteer_mission_history_datetime_placeholder_third),
                "https://images.unsplash.com/photo-1506744038136-46273834b3fb"));

        return missions;
    }

    /**
     * Lightweight UI model for presenting volunteer mission history items.
     */
    public static class MissionHistory {
        private final String type;
        private final String location;
        private final String time;
        private final String imageUrl;

        public MissionHistory(@NonNull String type,
                @NonNull String location,
                @NonNull String time,
                @NonNull String imageUrl) {
            this.type = type;
            this.location = location;
            this.time = time;
            this.imageUrl = imageUrl;
        }

        @NonNull
        public String getType() {
            return type;
        }

        @NonNull
        public String getLocation() {
            return location;
        }

        @NonNull
        public String getTime() {
            return time;
        }

        @NonNull
        public String getImageUrl() {
            return imageUrl;
        }
    }

    /**
     * Temporary adapter for rendering mission history cards in this fragment.
     */
    private class MissionHistoryAdapter extends RecyclerView.Adapter<MissionHistoryAdapter.MissionHistoryViewHolder> {

        private final List<MissionHistory> items;

        MissionHistoryAdapter(@NonNull List<MissionHistory> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public MissionHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemVolunteerMissionHistoryBinding itemBinding = ItemVolunteerMissionHistoryBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false);
            return new MissionHistoryViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull MissionHistoryViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class MissionHistoryViewHolder extends RecyclerView.ViewHolder {

            private final ItemVolunteerMissionHistoryBinding itemBinding;

            MissionHistoryViewHolder(@NonNull ItemVolunteerMissionHistoryBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }

            void bind(@NonNull MissionHistory mission) {
                itemBinding.tvMissionLocation.setText(mission.getLocation());
                itemBinding.tvMissionDateTime.setText(mission.getTime());

                if (TYPE_RESCUE.equalsIgnoreCase(mission.getType())) {
                    itemBinding.tvMissionCategory.setText(R.string.volunteer_mission_history_category_emergency);
                    itemBinding.cardMissionCategoryBadge.setCardBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.sos_red));
                } else {
                    itemBinding.tvMissionCategory.setText(R.string.volunteer_mission_history_category_supply);
                    itemBinding.cardMissionCategoryBadge.setCardBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.hub_blue));
                }

                Glide.with(itemBinding.ivMissionPhoto)
                        .load(mission.getImageUrl())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .centerCrop()
                        .into(itemBinding.ivMissionPhoto);
            }
        }
    }
}
