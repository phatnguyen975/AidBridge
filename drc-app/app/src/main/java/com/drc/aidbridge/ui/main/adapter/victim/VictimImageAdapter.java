package com.drc.aidbridge.ui.main.adapter.victim;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.databinding.ItemVictimImageBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying uploaded scene images in the Victim Rescue tab.
 */
public class VictimImageAdapter extends RecyclerView.Adapter<VictimImageAdapter.ImageViewHolder> {

    // TODO: Replace Integer with a proper data class if images are loaded from URIs or network
    private final List<Integer> images = new ArrayList<>();

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemVictimImageBinding binding = ItemVictimImageBinding.inflate(inflater, parent, false);
        return new ImageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        int imageRes = images.get(position);
        holder.binding.ivImage.setImageResource(imageRes);
        holder.binding.btnRemove.setOnClickListener(v -> removeAt(holder.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public void addImage(@DrawableRes int drawableRes) {
        images.add(drawableRes);
        notifyItemInserted(images.size() - 1);
    }

    private void removeAt(int position) {
        if (position == RecyclerView.NO_POSITION || position < 0 || position >= images.size()) {
            return;
        }
        images.remove(position);
        notifyItemRemoved(position);
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        final ItemVictimImageBinding binding;

        ImageViewHolder(ItemVictimImageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
