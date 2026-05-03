package com.drc.aidbridge.ui.map.feature.hub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.dto.response.hub.HubDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HubSearchAdapter extends RecyclerView.Adapter<HubSearchAdapter.HubViewHolder> {

    private final List<HubDto> hubs = new ArrayList<>();
    private final OnHubClickListener listener;

    public interface OnHubClickListener {
        void onHubClick(HubDto hub);
    }

    public HubSearchAdapter(OnHubClickListener listener) {
        this.listener = listener;
    }

    public void setHubs(List<HubDto> newHubs) {
        hubs.clear();
        if (newHubs != null) {
            hubs.addAll(newHubs);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hub_search_result, parent, false);
        return new HubViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HubViewHolder holder, int position) {
        HubDto hub = hubs.get(position);
        holder.bind(hub);
    }

    @Override
    public int getItemCount() {
        return hubs.size();
    }

    class HubViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvHubName;
        private final TextView tvHubDistance;
        private final TextView tvHubAddress;
        private final TextView tvHubOperatingHours;

        public HubViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHubName = itemView.findViewById(R.id.tvHubName);
            tvHubDistance = itemView.findViewById(R.id.tvHubDistance);
            tvHubAddress = itemView.findViewById(R.id.tvHubAddress);
            tvHubOperatingHours = itemView.findViewById(R.id.tvHubOperatingHours);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onHubClick(hubs.get(pos));
                }
            });
        }

        public void bind(HubDto hub) {
            tvHubName.setText(hub.getName() != null ? hub.getName() : "Không rõ tên");
            tvHubAddress.setText(hub.getAddress() != null ? hub.getAddress() : "Không có địa chỉ");
            tvHubOperatingHours.setText("Giờ HĐ: " + (hub.getOperatingHours() != null ? hub.getOperatingHours() : "--"));

            if (hub.getDistanceInMeters() != null) {
                double dist = hub.getDistanceInMeters();
                if (dist >= 1000) {
                    tvHubDistance.setText(String.format(Locale.getDefault(), "%.1f km", dist / 1000.0));
                } else {
                    tvHubDistance.setText(String.format(Locale.getDefault(), "%d m", Math.round(dist)));
                }
            } else {
                tvHubDistance.setText("");
            }
        }
    }
}
