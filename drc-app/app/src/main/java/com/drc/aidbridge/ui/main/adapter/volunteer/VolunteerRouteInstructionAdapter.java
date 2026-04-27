package com.drc.aidbridge.ui.main.adapter.volunteer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.dto.response.RoutingResponseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VolunteerRouteInstructionAdapter
        extends RecyclerView.Adapter<VolunteerRouteInstructionAdapter.InstructionViewHolder> {

    private final List<RoutingResponseDto.InstructionDto> instructions = new ArrayList<>();

    public void submitList(List<RoutingResponseDto.InstructionDto> newInstructions) {
        instructions.clear();
        if (newInstructions != null) {
            instructions.addAll(newInstructions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InstructionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_volunteer_route_instruction, parent, false);
        return new InstructionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InstructionViewHolder holder, int position) {
        holder.bind(instructions.get(position), position + 1);
    }

    @Override
    public int getItemCount() {
        return instructions.size();
    }

    static class InstructionViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvOrder;
        private final TextView tvCommand;
        private final TextView tvMeta;

        InstructionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrder = itemView.findViewById(R.id.tvInstructionOrder);
            tvCommand = itemView.findViewById(R.id.tvInstructionCommand);
            tvMeta = itemView.findViewById(R.id.tvInstructionMeta);
        }

        void bind(RoutingResponseDto.InstructionDto item, int order) {
            Context context = itemView.getContext();
            tvOrder.setText(String.format(Locale.getDefault(), "%d", order));

            String command = buildCommandWithRoad(context, item);
            tvCommand.setText(command);

            String streetName = item != null && item.getName() != null ? item.getName().trim() : "";
            String road = streetName.isEmpty()
                    ? context.getString(R.string.base_map_instruction_unnamed_road)
                    : streetName;

            double distanceMeters = item != null && item.getDistance() != null ? item.getDistance() : 0d;
            long seconds = item != null && item.getTime() != null ? Math.max(item.getTime() / 1000L, 0L) : 0L;

            String meta = context.getString(
                    R.string.base_map_instruction_meta,
                    formatDistance(context, distanceMeters),
                    formatDuration(context, seconds),
                    road
            );
            tvMeta.setText(meta);
        }

        private String buildCommandWithRoad(Context context, RoutingResponseDto.InstructionDto item) {
            String command = item != null && item.getCommand() != null && !item.getCommand().trim().isEmpty()
                    ? item.getCommand().trim()
                    : context.getString(R.string.base_map_instruction_continue);

            String road = item != null && item.getName() != null ? item.getName().trim() : "";
            if (road.isEmpty()) {
                return command;
            }

            return context.getString(R.string.base_map_instruction_with_road, command, road);
        }

        private String formatDistance(Context context, double meters) {
            if (meters >= 1000d) {
                return context.getString(R.string.base_map_distance_km, meters / 1000d);
            }
            return context.getString(R.string.base_map_distance_m, Math.round(meters));
        }

        private String formatDuration(Context context, long seconds) {
            long minutes = seconds / 60L;
            long remainSeconds = seconds % 60L;
            if (minutes > 0L) {
                return context.getString(R.string.base_map_duration_min_sec, minutes, remainSeconds);
            }
            return context.getString(R.string.base_map_duration_seconds, remainSeconds);
        }
    }
}
