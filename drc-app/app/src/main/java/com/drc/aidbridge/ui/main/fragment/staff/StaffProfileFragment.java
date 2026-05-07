package com.drc.aidbridge.ui.main.fragment.staff;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.FragmentStaffProfileBinding;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.MainActivity;
import com.drc.aidbridge.ui.main.viewmodel.staff.StaffProfileViewModel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffProfileFragment extends BaseFragment<FragmentStaffProfileBinding> {

    private static final String TAG = "AidBridgeSmsGateway";
    private static final DateTimeFormatter JOIN_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());

    private ActivityResultLauncher<String> receiveSmsPermissionLauncher;
    private StaffProfileViewModel viewModel;

    @Nullable
    @Override
    protected FragmentStaffProfileBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(StaffProfileViewModel.class);
        setupPermissionLauncher();
        binding.switchHubStatus.setChecked(true);
        updateGatewayStatusUi(hasReceiveSmsPermission());

        binding.switchHubStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !hasReceiveSmsPermission()) {
                receiveSmsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS);
                return;
            }
            updateGatewayStatusUi(isChecked && hasReceiveSmsPermission());
        });

        binding.btnLogout.setOnClickListener(v -> requestLogout());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadProfile();
        }
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUserLiveData().observe(getViewLifecycleOwner(),
                resultObserver(this::bindUserProfile, this::showLoadError));
        viewModel.getStaffStartDateLiveData().observe(getViewLifecycleOwner(), this::bindStaffStartDate);
    }

    private void bindUserProfile(@Nullable User user) {
        if (user == null) {
            return;
        }

        String displayName = safeOrFallback(
                user.getName(),
                safeOrFallback(user.getPhone(), getString(R.string.staff_profile_name)));

        binding.tvProfileName.setText(displayName);
        binding.tvProfileRole.setText(R.string.staff_profile_role);
        binding.tvPhoneValue.setText(safeOrFallback(user.getPhone(), getString(R.string.staff_profile_phone_value)));
        binding.tvEmailValue.setText(safeOrFallback(user.getEmail(), getString(R.string.staff_profile_email_value)));
        binding.tvJoinDateValue.setText(formatJoinDate(user.getCreatedAt()));
        viewModel.loadStaffStartDate(user.getId());

        String avatarUrl = trimToNull(user.getAvatarUrl());
        if (avatarUrl != null) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(binding.ivAvatar);
        } else {
            binding.ivAvatar.setImageResource(R.drawable.ic_avatar);
        }
    }

    private void bindStaffStartDate(NetworkResultWrapper<String> result) {
        if (result == null || !result.isSuccess()) {
            return;
        }

        String startDate = trimToNull(result.getData());
        if (startDate != null) {
            binding.tvJoinDateValue.setText(formatJoinDate(startDate));
        }
    }

    private void showLoadError(String message) {
        showTopSnackbar(binding.getRoot(), message, true);
    }

    private String formatJoinDate(@Nullable String rawDateTime) {
        String safeRawDateTime = trimToNull(rawDateTime);
        if (safeRawDateTime == null) {
            return getString(R.string.staff_profile_join_date_value);
        }

        try {
            return JOIN_DATE_FORMATTER
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.parse(safeRawDateTime));
        } catch (DateTimeParseException exception) {
            try {
                return JOIN_DATE_FORMATTER.format(LocalDate.parse(safeRawDateTime));
            } catch (DateTimeParseException ignored) {
                return safeRawDateTime;
            }
        }
    }

    private String safeOrFallback(@Nullable String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed != null ? trimmed : fallback;
    }

    @Nullable
    private String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void setupPermissionLauncher() {
        receiveSmsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    boolean granted = Boolean.TRUE.equals(isGranted);
                    if (!granted) {
                        Log.w(TAG, "SMS_GATEWAY_RECEIVE_PERMISSION_DENIED");
                        showTopSnackbar(
                                binding.getRoot(),
                                getString(R.string.staff_gateway_sms_permission_denied),
                                true);
                    }
                    binding.switchHubStatus.setChecked(granted);
                    updateGatewayStatusUi(granted);
                });
    }

    private void updateGatewayStatusUi(boolean isReady) {
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

        if (!hasReceiveSmsPermission()) {
            binding.tvReadyStatus.setText(R.string.staff_gateway_sms_permission_missing);
            binding.tvOfflineStatus.setText(R.string.staff_profile_offline);
        } else {
            binding.tvReadyStatus.setText(R.string.staff_profile_ready);
            binding.tvOfflineStatus.setText(R.string.staff_profile_offline);
        }
    }

    private boolean hasReceiveSmsPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private GradientDrawable createIndicatorBackground(int strokeColorRes, int fillColorRes) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(getResources().getDimension(R.dimen.staff_status_indicator_corner));
        drawable.setStroke(
                getResources().getDimensionPixelSize(R.dimen.staff_status_indicator_stroke),
                ContextCompat.getColor(requireContext(), strokeColorRes));
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
                strokeColor);
        return drawable;
    }

    private void requestLogout() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).requestLogout();
        }
    }
}
