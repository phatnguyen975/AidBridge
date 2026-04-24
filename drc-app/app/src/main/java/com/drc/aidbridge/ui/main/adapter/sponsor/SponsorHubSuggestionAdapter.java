package com.drc.aidbridge.ui.main.adapter.sponsor;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemSponsorHubSuggestionBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * SponsorHubSuggestionAdapter renders selectable hub suggestions for Sponsor bottom sheet.
 */
public class SponsorHubSuggestionAdapter extends RecyclerView.Adapter<SponsorHubSuggestionAdapter.HubSuggestionViewHolder> {

    private final List<HubSuggestionItem> items = new ArrayList<>();
    private int selectedPosition = 0;

    @NonNull
    @Override
    public HubSuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemSponsorHubSuggestionBinding binding = ItemSponsorHubSuggestionBinding.inflate(inflater, parent, false);
        return new HubSuggestionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HubSuggestionViewHolder holder, int position) {
        HubSuggestionItem item = items.get(position);
        boolean isSelected = position == selectedPosition;

        holder.binding.tvHubName.setText(item.hubName);
        holder.binding.tvHubDistance.setText(item.distanceText);
        holder.binding.chipUrgency.setVisibility(item.showUrgency ? android.view.View.VISIBLE : android.view.View.GONE);

        int strokeColorRes = isSelected ? R.color.color_primary : R.color.border_default;
        int strokeWidthRes = isSelected
                ? R.dimen.sponsor_hub_item_selected_stroke_width
                : R.dimen.sponsor_hub_item_stroke_width;

        holder.binding.cardHubSuggestion.setStrokeColor(
                ColorStateList.valueOf(ContextCompat.getColor(holder.binding.getRoot().getContext(), strokeColorRes))
        );
        holder.binding.cardHubSuggestion.setStrokeWidth(
                holder.binding.getRoot().getResources().getDimensionPixelSize(strokeWidthRes)
        );

        holder.binding.getRoot().setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            if (oldPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPosition);
            }
            notifyItemChanged(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitItems(@NonNull List<HubSuggestionItem> newItems) {
        items.clear();
        items.addAll(newItems);
        selectedPosition = 0;
        notifyDataSetChanged();
    }

    @NonNull
    public HubSuggestionItem getSelectedItem() {
        if (items.isEmpty()) {
            return new HubSuggestionItem("", "", "", false);
        }
        int safePosition = Math.max(0, Math.min(selectedPosition, items.size() - 1));
        return items.get(safePosition);
    }

    public static class HubSuggestionItem {
        public final String hubId;
        public final String hubName;
        public final String distanceText;
        public final boolean showUrgency;

        public HubSuggestionItem(@NonNull String hubId,
                                 @NonNull String hubName,
                                 @NonNull String distanceText,
                                 boolean showUrgency) {
            this.hubId = hubId;
            this.hubName = hubName;
            this.distanceText = distanceText;
            this.showUrgency = showUrgency;
        }
    }

    static class HubSuggestionViewHolder extends RecyclerView.ViewHolder {
        final ItemSponsorHubSuggestionBinding binding;

        HubSuggestionViewHolder(@NonNull ItemSponsorHubSuggestionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
