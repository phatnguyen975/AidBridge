package com.drc.aidbridge.ui.map.base.helper;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.ui.map.base.BaseMapViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class RouteOptionsHelper {

    public interface RouteOptionsListener {
        void onApplyOptions(@NonNull String strategy, boolean avoidDangerousZones);
    }

    @Nullable
    private RouteOptionsListener listener;

    public void setListener(@Nullable RouteOptionsListener listener) {
        this.listener = listener;
    }

    public void showRouteOptionBottomSheet(
            @NonNull android.content.Context context,
            @NonNull String currentStrategy,
            boolean avoidDangerousZones) {

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setCanceledOnTouchOutside(false);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_volunteer_route_options, null, false);
        dialog.setContentView(view);

        RadioGroup radioGroup = view.findViewById(R.id.rgStrategy);
        SwitchMaterial avoidSwitch = view.findViewById(R.id.switchAvoidDangerous);
        MaterialButton applyButton = view.findViewById(R.id.btnApplyOptions);

        radioGroup.check(resolveRadioByStrategy(currentStrategy));
        avoidSwitch.setChecked(avoidDangerousZones);

        applyButton.setOnClickListener(v -> {
            String selectedStrategy = resolveStrategyByRadio(radioGroup.getCheckedRadioButtonId());
            boolean nextAvoid = avoidSwitch.isChecked();

            if (listener != null) {
                listener.onApplyOptions(selectedStrategy, nextAvoid);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    public int resolveRadioByStrategy(@NonNull String strategy) {
        switch (strategy) {
            case BaseMapViewModel.STRATEGY_URGENT:
                return R.id.rbStrategyUrgent;
            case BaseMapViewModel.STRATEGY_SAFE:
                return R.id.rbStrategySafe;
            case BaseMapViewModel.STRATEGY_HEAVY_AID:
                return R.id.rbStrategyHeavyAid;
            case BaseMapViewModel.STRATEGY_COMMUNITY:
                return R.id.rbStrategyCommunity;
            default:
                return R.id.rbStrategyOffroad;
        }
    }

    @NonNull
    public String resolveStrategyByRadio(int checkedId) {
        if (checkedId == R.id.rbStrategyUrgent) {
            return BaseMapViewModel.STRATEGY_URGENT;
        }
        if (checkedId == R.id.rbStrategySafe) {
            return BaseMapViewModel.STRATEGY_SAFE;
        }
        if (checkedId == R.id.rbStrategyHeavyAid) {
            return BaseMapViewModel.STRATEGY_HEAVY_AID;
        }
        if (checkedId == R.id.rbStrategyCommunity) {
            return BaseMapViewModel.STRATEGY_COMMUNITY;
        }
        return BaseMapViewModel.STRATEGY_OFFROAD;
    }
}
