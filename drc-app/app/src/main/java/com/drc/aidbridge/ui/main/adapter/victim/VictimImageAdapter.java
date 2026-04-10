package com.drc.aidbridge.ui.main.adapter.victim;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemVictimImageBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying uploaded scene images in the Victim Rescue tab.
 */
public class VictimImageAdapter extends RecyclerView.Adapter<VictimImageAdapter.ImageViewHolder> {

    private final List<Uri> images = new ArrayList<>();
    private final OnImageRemovedListener onImageRemovedListener;

    public VictimImageAdapter(@NonNull OnImageRemovedListener onImageRemovedListener) {
        this.onImageRemovedListener = onImageRemovedListener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemVictimImageBinding binding = ItemVictimImageBinding.inflate(inflater, parent, false);
        return new ImageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri imageUri = images.get(position);
        Glide.with(holder.binding.ivImage)
            .load(imageUri)
            .placeholder(R.drawable.ic_rescue)
            .error(R.drawable.ic_rescue)
            .centerCrop()
            .into(holder.binding.ivImage);

        holder.binding.btnRemove.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION
                || adapterPosition < 0
                || adapterPosition >= images.size()) {
                return;
            }

            Uri removedImage = images.get(adapterPosition);
            images.remove(adapterPosition);
            notifyItemRemoved(adapterPosition);
            onImageRemovedListener.onImageRemoved(removedImage);
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public void submitImages(List<Uri> newImages) {
        images.clear();
        if (newImages != null) {
            images.addAll(newImages);
        }
        notifyDataSetChanged();
    }

    public interface OnImageRemovedListener {
        void onImageRemoved(@NonNull Uri imageUri);
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        final ItemVictimImageBinding binding;

        ImageViewHolder(ItemVictimImageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
