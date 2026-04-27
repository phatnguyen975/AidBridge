package com.drc.aidbridge.ui.map.base.helper;

import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drc.aidbridge.BuildConfig;
import com.drc.aidbridge.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.osmdroid.util.GeoPoint;

public class DevPanelHelper {

    public interface DevPanelListener {
        void onApplyCustomEndPoint(@NonNull GeoPoint endPoint);
        void onScenario2KmRequested();
        void onNetworkDropToggled(boolean isNetworkDropSimulated);
        void onClearMapRequested();
        void onError(@NonNull String message);
    }

    @Nullable
    private DevPanelListener listener;

    public void setListener(@Nullable DevPanelListener listener) {
        this.listener = listener;
    }

    public void showDevPanelBottomSheet(
            @NonNull android.content.Context context,
            boolean isNetworkDropSimulated) {
        
        if (!BuildConfig.DEBUG) {
            return;
        }

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

        btnApplyCoordinates.setOnClickListener(v -> {
            String input = etCoordinates.getText() != null ? etCoordinates.getText().toString().trim() : "";
            GeoPoint parsed = parseCoordinateInput(input);
            if (parsed == null) {
                if (listener != null) {
                    listener.onError(context.getString(R.string.base_map_dev_invalid_coordinates));
                }
                return;
            }

            if (listener != null) {
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

        btnScenarioNetwork.setOnClickListener(v -> {
            boolean nextState = !isNetworkDropSimulated;
            if (listener != null) {
                listener.onNetworkDropToggled(nextState);
            }
            dialog.dismiss();
        });

        btnClear.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClearMapRequested();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    @Nullable
    public GeoPoint parseCoordinateInput(@Nullable String input) {
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
