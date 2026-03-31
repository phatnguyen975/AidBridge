package com.drc.aidbridge.ui.main.adapter.volunteer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemVolunteerMissionHistoryBinding;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * VolunteerMissionHistoryAdapter renders volunteer mission history cards.
 */
public class VolunteerMissionHistoryAdapter
        extends RecyclerView.Adapter<VolunteerMissionHistoryAdapter.VolunteerMissionHistoryViewHolder> {

    private final List<MissionHistoryModel> missionList = new ArrayList<>();

    @Inject
    public VolunteerMissionHistoryAdapter() {
    }

    @NonNull
    @Override
    public VolunteerMissionHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemVolunteerMissionHistoryBinding binding = ItemVolunteerMissionHistoryBinding.inflate(inflater, parent,
                false);
        return new VolunteerMissionHistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VolunteerMissionHistoryViewHolder holder, int position) {
        MissionHistoryModel mission = missionList.get(position);

        holder.binding.tvMissionCategory.setText(mission.getType());
        holder.binding.tvMissionLocation.setText(mission.getLocation());
        holder.binding.tvMissionDateTime.setText(mission.getTime());

        if (holder.binding.getRoot().getContext().getString(R.string.volunteer_history_filter_rescue)
                .equalsIgnoreCase(mission.getType())) {
            holder.binding.cardMissionCategoryBadge.setCardBackgroundColor(
                    holder.binding.getRoot().getContext().getColor(R.color.sos_red));
        } else {
            holder.binding.cardMissionCategoryBadge.setCardBackgroundColor(
                    holder.binding.getRoot().getContext().getColor(R.color.hub_blue));
        }

        Glide.with(holder.binding.ivMissionPhoto)
                .load(mission.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(holder.binding.ivMissionPhoto);
    }

    @Override
    public int getItemCount() {
        return missionList.size();
    }

    public void setMissionList(@NonNull List<MissionHistoryModel> missions) {
        missionList.clear();
        missionList.addAll(missions);
        notifyDataSetChanged();
    }

    public void addItems(@NonNull List<MissionHistoryModel> newItems) {
        if (newItems.isEmpty()) {
            return;
        }
        int start = missionList.size();
        missionList.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    public void clear() {
        missionList.clear();
        notifyDataSetChanged();
    }

    public static class MissionHistoryModel {
        private String type;
        private String location;
        private String time;
        private String imageUrl;

        public MissionHistoryModel() {
        }

        public MissionHistoryModel(String type, String location, String time, String imageUrl) {
            this.type = type;
            this.location = location;
            this.time = time;
            this.imageUrl = imageUrl;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    static class VolunteerMissionHistoryViewHolder extends RecyclerView.ViewHolder {
        final ItemVolunteerMissionHistoryBinding binding;

        VolunteerMissionHistoryViewHolder(ItemVolunteerMissionHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
