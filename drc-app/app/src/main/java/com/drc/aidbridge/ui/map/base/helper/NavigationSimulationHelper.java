package com.drc.aidbridge.ui.map.base.helper;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drc.aidbridge.BuildConfig;
import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentMapBaseBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.osmdroid.util.GeoPoint;

public class NavigationSimulationHelper {

    public interface SimulationListener {
        void onSimulationStartRequested();
        void onSimulationStopRequested();
        void onApplyCustomEndPoint(@NonNull GeoPoint endPoint);
        void onScenario2KmRequested();
        void onToggleNetworkSimulation();
        void onClearMapState();
    }

    private SimulationListener listener;
    private long lastAvatarTapAt = 0L;
    private int avatarTapCount = 0;
    private static final long DEV_TAP_WINDOW_MS = 1500L;

    public void setup(@NonNull Activity activity,
                      @NonNull FragmentMapBaseBinding binding,
                      boolean isNetworkDropSimulated,
                      @NonNull SimulationListener listener) {
        this.listener = listener;

        boolean isDebug = BuildConfig.DEBUG;
        binding.btnDevSimulate.setVisibility(isDebug ? View.VISIBLE : View.GONE);
        binding.ivDevAvatar.setVisibility(isDebug ? View.VISIBLE : View.GONE);
        binding.tvDevHintSign.setVisibility(isDebug ? View.VISIBLE : View.GONE);

        if (!isDebug) {
            return;
        }

        binding.btnDevSimulate.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSimulationStartRequested();
            }
        });

        binding.ivDevAvatar.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - lastAvatarTapAt > DEV_TAP_WINDOW_MS) {
                avatarTapCount = 0;
            }
            avatarTapCount++;
            lastAvatarTapAt = now;
            if (avatarTapCount >= 5) {
                avatarTapCount = 0;
                showDevPanelBottomSheet(activity, isNetworkDropSimulated);
            }
        });
    }

    public void updateSimulationButtonState(@NonNull FragmentMapBaseBinding binding, boolean isRunning) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        binding.btnDevSimulate.setText(isRunning
                ? R.string.base_map_dev_simulate_stop
                : R.string.base_map_dev_simulate);
    }

    private void showDevPanelBottomSheet(@NonNull Context context, boolean isNetworkDropSimulated) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setCanceledOnTouchOutside(false);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_volunteer_dev_panel, null, false);
        dialog.setContentView(view);

        TextInputEditText etCoordinates = view.findViewById(R.id.etDevCoordinates);
        MaterialButton btnApplyCoordinates = view.findViewById(R.id.btnDevApplyCoordinates);
        MaterialButton btnScenario2Km = view.findViewById(R.id.btnDevScenario2km);
        MaterialButton btnScenarioNetwork = view.findViewById(R.id.btnDevScenarioNetworkDrop);
        MaterialButton btnClear = view.findViewById(R.id.btnDevClearMarkers);

        btnScenarioNetwork.setText(isNetworkDropSimulated
                ? R.string.base_map_dev_scenario_network_restore
                : R.string.base_map_dev_scenario_network_drop);

        btnScenarioNetwork.setOnClickListener(v -> {
            if (listener != null) {
                listener.onToggleNetworkSimulation();
            }
            dialog.dismiss();
        });

        btnApplyCoordinates.setOnClickListener(v -> {
            String input = etCoordinates.getText() != null ? etCoordinates.getText().toString().trim() : "";
            GeoPoint parsed = parseCoordinateInput(input);
            if (parsed != null && listener != null) {
                listener.onApplyCustomEndPoint(parsed);
            }
            dialog.dismiss();
        });

        btnScenario2Km.setOnClickListener(v -> {
            if (listener != null) {
                listener.onScenario2KmRequested();
            }
            dialog.dismiss();
        });

        btnClear.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClearMapState();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    @Nullable
    private GeoPoint parseCoordinateInput(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        String[] parts = input.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new GeoPoint(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
