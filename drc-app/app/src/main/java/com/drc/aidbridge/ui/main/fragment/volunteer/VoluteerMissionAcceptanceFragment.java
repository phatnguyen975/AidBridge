package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerMissionAcceptanceBinding;
import com.drc.aidbridge.domain.model.VolunteerMission;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.MainActivity;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerTaskViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.Instant;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VoluteerMissionAcceptanceFragment extends BaseFragment<FragmentVolunteerMissionAcceptanceBinding> {

    public static final String ARG_MISSION_ID = "missionId";
    public static final String ARG_DISPATCH_ATTEMPT_ID = "dispatchAttemptId";
    public static final String ARG_MISSION_TYPE = "missionType";
    public static final String ARG_EXPIRES_AT = "expiresAt";
    public static final String MISSION_TYPE_RESCUE = "RESCUE";
    public static final String MISSION_TYPE_DELIVERY = "DELIVERY";
    public static final String MISSION_TYPE_SUPPLY = "SUPPLY";

    private static final long TIMER_INTERVAL_MS = 1_000L;
    private static final long FALLBACK_TIMER_TOTAL_MS = 60_000L;

    private VolunteerTaskViewModel volunteerTaskViewModel;
    private CountDownTimer missionCountDownTimer;

    @NonNull
    public static VoluteerMissionAcceptanceFragment newInstance(@NonNull String missionType) {
        VoluteerMissionAcceptanceFragment fragment = new VoluteerMissionAcceptanceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MISSION_TYPE, missionType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected FragmentVolunteerMissionAcceptanceBinding inflateBinding(LayoutInflater inflater,
                                                                      @Nullable ViewGroup container) {
        return FragmentVolunteerMissionAcceptanceBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        volunteerTaskViewModel = new ViewModelProvider(requireActivity()).get(VolunteerTaskViewModel.class);
        hydrateDispatchContextFromArguments();
        applyMissionTypeUI(resolveMissionType());
        setupClickListeners();
        startCountDownTimer(resolveExpiresAt());
        VolunteerMission existingMission = volunteerTaskViewModel.getPendingMission().getValue();
        if (existingMission != null) {
            renderMission(existingMission);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isAdded() || getActivity() == null) {
            return;
        }

        BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            MenuItem missionItem = bottomNav.getMenu().findItem(R.id.volunteerMissionListFragment);
            if (missionItem != null) {
                missionItem.setChecked(true);
            }
        }
    }

    @Override
    protected void observeViewModel() {
        volunteerTaskViewModel.getPendingMission().observe(getViewLifecycleOwner(), mission -> {
            if (mission != null) {
                renderMission(mission);
                applyMissionTypeUI(mission.getMissionType());
            }
        });

        volunteerTaskViewModel.getPendingMissionResult().observe(
                getViewLifecycleOwner(),
                resultObserver(this::renderMission, this::showLoadError)
        );

        volunteerTaskViewModel.getAcceptResult().observe(
                getViewLifecycleOwner(),
                resultObserver(this::handleAcceptSuccess, this::showActionError)
        );

        volunteerTaskViewModel.getRejectResult().observe(
                getViewLifecycleOwner(),
                resultObserver(this::handleRejectSuccess, this::showActionError)
        );
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        binding.btnAccept.setEnabled(!isLoading);
        binding.btnDecline.setEnabled(!isLoading);
        binding.btnAccept.setAlpha(isLoading ? 0.6f : 1f);
        binding.btnDecline.setAlpha(isLoading ? 0.6f : 1f);
    }

    @Override
    public void onDestroyView() {
        cancelCountDownTimer();
        super.onDestroyView();
    }

    private void hydrateDispatchContextFromArguments() {
        Bundle args = getArguments();
        if (args == null) {
            if (!volunteerTaskViewModel.hasPendingDispatch()) {
                showToast(getString(R.string.volunteer_mission_acceptance_no_dispatch));
                popBackStackSafely();
            }
            return;
        }

        String missionId = args.getString(ARG_MISSION_ID);
        String dispatchAttemptId = args.getString(ARG_DISPATCH_ATTEMPT_ID);
        String missionType = args.getString(ARG_MISSION_TYPE);
        String expiresAt = args.getString(ARG_EXPIRES_AT);
        if (missionId != null && dispatchAttemptId != null) {
            volunteerTaskViewModel.openDispatchRequest(missionId, dispatchAttemptId, missionType, expiresAt);
        } else if (!volunteerTaskViewModel.hasPendingDispatch()) {
            showToast(getString(R.string.volunteer_mission_acceptance_no_dispatch));
            popBackStackSafely();
        }
    }

    private void setupClickListeners() {
        binding.btnAccept.setOnClickListener(v -> volunteerTaskViewModel.acceptPendingMission());
        binding.btnDecline.setOnClickListener(v -> showRejectReasonDialog());
    }

    private void showRejectReasonDialog() {
        String[] labels = new String[] {
                getString(R.string.volunteer_mission_reject_reason_busy),
                getString(R.string.volunteer_mission_reject_reason_too_far),
                getString(R.string.volunteer_mission_reject_reason_personal),
                getString(R.string.volunteer_mission_reject_reason_other)
        };
        String[] values = new String[] { "BUSY", "TOO_FAR", "PERSONAL", "OTHER" };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.volunteer_mission_reject_reason_title)
                .setItems(labels, (dialog, which) ->
                        volunteerTaskViewModel.rejectPendingMission(values[which], null))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void applyMissionTypeUI(@Nullable String missionType) {
        if (isDeliveryMissionType(missionType)) {
            binding.cardMissionBadge.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.hub_blue)
            );
            binding.tvMissionBadge.setText(R.string.volunteer_mission_acceptance_mission_badge_supply);
            binding.tvMissionTitle.setText(R.string.volunteer_sos_acceptance_mission_title_supply);
            binding.cardSupplyItems.setVisibility(View.VISIBLE);
            return;
        }

        binding.cardMissionBadge.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.sos_red)
        );
        binding.tvMissionBadge.setText(R.string.volunteer_sos_acceptance_mission_badge);
        binding.tvMissionTitle.setText(R.string.volunteer_sos_acceptance_mission_title);
        binding.cardSupplyItems.setVisibility(View.GONE);
    }

    private void renderMission(@Nullable VolunteerMission mission) {
        if (mission == null || binding == null) {
            return;
        }

        String missionType = mission.getMissionType();
        binding.tvVictimValue.setText(
                isDeliveryMissionType(missionType)
                        ? getString(R.string.volunteer_mission_acceptance_delivery_subject)
                        : getString(R.string.volunteer_mission_acceptance_rescue_subject)
        );
        binding.tvAddressValue.setText(resolveAddress(mission));
        binding.tvAddressNote.setText(resolveNote(mission));
        binding.tvDistanceValue.setText(resolveCoordinatesLabel(mission));
        binding.tvSupplyItemsValue.setText(resolveSupplyItems(mission));

        Instant expiresAt = mission.getExpiresAt();
        if (expiresAt != null) {
            startCountDownTimer(expiresAt);
        }
    }

    private void handleAcceptSuccess(@Nullable VolunteerMission mission) {
        String missionType = mission != null ? mission.getMissionType() : resolveMissionType();
        showToast(getString(R.string.volunteer_sos_acceptance_toast_accept_success));
        if (isDeliveryMissionType(missionType)) {
            navigateSafely(R.id.action_sos_acceptance_to_delivery_mission);
            return;
        }
        navigateSafely(R.id.action_sos_acceptance_to_current_sos_mission);
    }

    private void handleRejectSuccess(@Nullable Boolean ignored) {
        showToast(getString(R.string.volunteer_sos_acceptance_toast_decline_success));
        popBackStackSafely();
    }

    private void showLoadError(@NonNull String message) {
        showTopSnackbar(binding.getRoot(), message, true);
    }

    private void showActionError(@NonNull String message) {
        showTopSnackbar(binding.getRoot(), message, true);
    }

    private void startCountDownTimer(@Nullable Instant expiresAt) {
        cancelCountDownTimer();

        long durationMs = FALLBACK_TIMER_TOTAL_MS;
        if (expiresAt != null) {
            long computed = expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
            durationMs = Math.max(computed, 0L);
        }

        updateCountdownViews(durationMs);
        if (durationMs <= 0L) {
            volunteerTaskViewModel.expirePendingDispatch();
            showToast(getString(R.string.volunteer_mission_acceptance_expired));
            popBackStackSafely();
            return;
        }

        missionCountDownTimer = new CountDownTimer(durationMs, TIMER_INTERVAL_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateCountdownViews(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                updateCountdownViews(0L);
                volunteerTaskViewModel.expirePendingDispatch();
                showToast(getString(R.string.volunteer_mission_acceptance_expired));
                popBackStackSafely();
            }
        };
        missionCountDownTimer.start();
    }

    private void updateCountdownViews(long millisRemaining) {
        long totalSeconds = Math.max(millisRemaining / 1000L, 0L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        binding.tvTimerMinutesValue.setText(String.format(Locale.getDefault(), "%02d", minutes));
        binding.tvTimerSecondsValue.setText(String.format(Locale.getDefault(), "%02d", seconds));
    }

    private void cancelCountDownTimer() {
        if (missionCountDownTimer != null) {
            missionCountDownTimer.cancel();
            missionCountDownTimer = null;
        }
    }

    @Nullable
    private Instant resolveExpiresAt() {
        VolunteerMission mission = volunteerTaskViewModel.getPendingMission().getValue();
        if (mission != null && mission.getExpiresAt() != null) {
            return mission.getExpiresAt();
        }

        Bundle args = getArguments();
        if (args == null) {
            return null;
        }
        String expiresAt = args.getString(ARG_EXPIRES_AT);
        if (expiresAt == null || expiresAt.trim().isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(expiresAt);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private String resolveMissionType() {
        VolunteerMission mission = volunteerTaskViewModel.getPendingMission().getValue();
        if (mission != null) {
            return mission.getMissionType();
        }
        Bundle args = getArguments();
        return args != null ? args.getString(ARG_MISSION_TYPE) : null;
    }

    private String resolveAddress(@NonNull VolunteerMission mission) {
        if (mission.getAddress() != null && !mission.getAddress().trim().isEmpty()) {
            return mission.getAddress();
        }
        if (mission.getVictimLat() != null && mission.getVictimLng() != null) {
            return getString(
                    R.string.volunteer_mission_acceptance_coordinates_fallback,
                    mission.getVictimLat(),
                    mission.getVictimLng()
            );
        }
        return getString(R.string.volunteer_mission_acceptance_address_fallback);
    }

    private String resolveNote(@NonNull VolunteerMission mission) {
        if (mission.getNote() != null && !mission.getNote().trim().isEmpty()) {
            return mission.getNote();
        }
        if (mission.getComment() != null && !mission.getComment().trim().isEmpty()) {
            return mission.getComment();
        }
        return getString(R.string.volunteer_mission_acceptance_note_fallback);
    }

    private String resolveSupplyItems(@NonNull VolunteerMission mission) {
        if (mission.getComment() != null && !mission.getComment().trim().isEmpty()) {
            return mission.getComment();
        }
        return getString(R.string.volunteer_mission_acceptance_supply_items_fallback);
    }

    private String resolveCoordinatesLabel(@NonNull VolunteerMission mission) {
        if (mission.getVictimLat() == null || mission.getVictimLng() == null) {
            return getString(R.string.volunteer_mission_acceptance_distance_unavailable);
        }
        return getString(
                R.string.volunteer_mission_acceptance_coordinates_badge,
                mission.getVictimLat(),
                mission.getVictimLng()
        );
    }

    public static boolean isDeliveryMissionType(@Nullable String missionType) {
        return MISSION_TYPE_DELIVERY.equalsIgnoreCase(missionType)
                || MISSION_TYPE_SUPPLY.equalsIgnoreCase(missionType);
    }
}
