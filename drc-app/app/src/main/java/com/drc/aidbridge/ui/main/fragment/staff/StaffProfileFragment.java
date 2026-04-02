package com.drc.aidbridge.ui.main.fragment.staff;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentStaffProfileBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.MainActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffProfileFragment extends BaseFragment<FragmentStaffProfileBinding> {

    @Nullable
    @Override
    protected FragmentStaffProfileBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        binding.switchHubStatus.setChecked(true);
        updateHubStatusUi(true);

        binding.switchHubStatus.setOnCheckedChangeListener((buttonView, isChecked) ->
            updateHubStatusUi(isChecked));

        binding.btnLogout.setOnClickListener(v -> requestLogout());
    }

    @Override
    protected void observeViewModel() {
    }

    private void updateHubStatusUi(boolean isReady) {
        int readyStroke = isReady ? R.color.staff_ready_color : R.color.staff_dim_color;
        int readyFill = isReady ? R.color.staff_ready_bg : android.R.color.transparent;
        int readyText = isReady ? R.color.staff_ready_color : R.color.staff_dim_color;

        int offlineStroke = isReady ? R.color.staff_offline_color : R.color.staff_offline_active;
        int offlineFill = isReady ? android.R.color.transparent : R.color.staff_offline_bg;
        int offlineText = isReady ? R.color.staff_offline_color : R.color.staff_offline_active;

        binding.llStatusReady.setBackground(createIndicatorBackground(readyStroke, readyFill));
        binding.llStatusOffline.setBackground(createIndicatorBackground(offlineStroke, offlineFill));

        binding.ivReadyIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), readyText)));
        binding.ivOfflineIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), offlineText)));

        binding.tvReadyStatus.setTextColor(ContextCompat.getColor(requireContext(), readyText));
        binding.tvOfflineStatus.setTextColor(ContextCompat.getColor(requireContext(), offlineText));

        int dotColorRes = isReady ? R.color.staff_active_dot_green : R.color.staff_active_dot_gray;
        binding.vActiveDot.setBackground(createActiveDotBackground(dotColorRes));

        int toggleThumbRes = isReady ? R.color.staff_toggle_thumb_ready : R.color.staff_toggle_thumb_offline;
        int toggleTrackRes = isReady ? R.color.staff_toggle_track_ready : R.color.staff_toggle_track_offline;
        binding.switchHubStatus.setThumbTintList(ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), toggleThumbRes)));
        binding.switchHubStatus.setTrackTintList(ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), toggleTrackRes)));
    }

    private GradientDrawable createIndicatorBackground(int strokeColorRes, int fillColorRes) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(getResources().getDimension(R.dimen.staff_status_indicator_corner));
        drawable.setStroke(
                getResources().getDimensionPixelSize(R.dimen.staff_status_indicator_stroke),
                ContextCompat.getColor(requireContext(), strokeColorRes)
        );
        drawable.setColor(ContextCompat.getColor(requireContext(), fillColorRes));
        return drawable;
    }

    private GradientDrawable createActiveDotBackground(int colorRes) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        int color = ContextCompat.getColor(requireContext(), colorRes);
        int strokeColor = ColorUtils.blendARGB(color, Color.BLACK, 0.28f);
        drawable.setColor(color);
        drawable.setStroke(
                getResources().getDimensionPixelSize(R.dimen.staff_active_dot_stroke_width),
            strokeColor
        );
        return drawable;
    }

    private void requestLogout() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).requestLogout();
        }
    }
}
