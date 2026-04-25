package com.drc.aidbridge.ui.main.adapter.sponsor;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemSponsorDonationItemBinding;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationItem;

import java.util.ArrayList;
import java.util.List;

public class SponsorDonationItemAdapter extends RecyclerView.Adapter<SponsorDonationItemAdapter.DonationItemViewHolder> {

    public interface ItemActionListener {
        void onRemove(int position);
    }

    private final List<SponsorDonationItem> items = new ArrayList<>();
    private final ItemActionListener itemActionListener;

    public SponsorDonationItemAdapter(@NonNull ItemActionListener itemActionListener) {
        this.itemActionListener = itemActionListener;
    }

    @NonNull
    @Override
    public DonationItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemSponsorDonationItemBinding binding = ItemSponsorDonationItemBinding.inflate(inflater, parent, false);
        return new DonationItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DonationItemViewHolder holder, int position) {
        SponsorDonationItem item = items.get(position);
        String displayName = safeText(item.getDisplayName());
        holder.binding.tvItemName.setText(displayName.isEmpty() ? safeText(item.getItemCategoryId()) : displayName);

        String categoryId = safeText(item.getItemCategoryId());
        String meta = categoryId.isEmpty()
                ? holder.binding.getRoot().getContext().getString(R.string.sponsor_donate_item_category_none)
                : categoryId;
        holder.binding.tvItemMeta.setText(meta);
        holder.binding.tvItemDescription.setText("");

        holder.binding.btnRemoveItem.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }
            itemActionListener.onRemove(adapterPosition);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitItems(@NonNull List<SponsorDonationItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }

    static class DonationItemViewHolder extends RecyclerView.ViewHolder {
        private final ItemSponsorDonationItemBinding binding;

        DonationItemViewHolder(@NonNull ItemSponsorDonationItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
