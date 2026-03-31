package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerMissionAcceptanceBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerTaskViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VoluteerMissionAcceptanceFragment extends BaseFragment<FragmentVolunteerMissionAcceptanceBinding> {

    public static final String ARG_MISSION_TYPE = "missionType";
    public static final String MISSION_TYPE_RESCUE = "RESCUE";
    public static final String MISSION_TYPE_SUPPLY = "SUPPLY";

    private static final long TIMER_TOTAL_MS = 60_000L;
    private static final long TIMER_INTERVAL_MS = 1_000L;
    private static final String MOCK_MISSION_ID = "MISSION_RESCUE_001";
    private static final String MOCK_SUPPLY_MISSION_ID = "MISSION_SUPPLY_001";

    private String missionType = MISSION_TYPE_RESCUE;
    private CountDownTimer missionCountDownTimer;
    private VolunteerTaskViewModel volunteerTaskViewModel;

    @NonNull
    public static VoluteerMissionAcceptanceFragment newInstance(@NonNull String missionType) {
        VoluteerMissionAcceptanceFragment fragment = new VoluteerMissionAcceptanceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MISSION_TYPE, missionType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String argType = getArguments().getString(ARG_MISSION_TYPE);
            if (argType != null) {
                missionType = argType;
            }
        }
    }

    @Override
    protected FragmentVolunteerMissionAcceptanceBinding inflateBinding(LayoutInflater inflater,
            @Nullable ViewGroup container) {
        return FragmentVolunteerMissionAcceptanceBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        volunteerTaskViewModel = new ViewModelProvider(requireActivity()).get(VolunteerTaskViewModel.class);
        applyMissionTypeUI();
        setupClickListeners();
        startCountDownTimer();
    }

    @Override
    protected void observeViewModel() {
        // TODO: Inject ViewModel va observe LiveData de cap nhat du lieu nhiem vu tu
        // UseCase.
    }

    @Override
    public void onDestroyView() {
        cancelCountDownTimer();
        super.onDestroyView();
    }

    private void applyMissionTypeUI() {
        if (MISSION_TYPE_SUPPLY.equalsIgnoreCase(missionType)) {
            binding.cardMissionBadge.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.hub_blue));
            binding.tvMissionBadge.setText(R.string.volunteer_mission_acceptance_mission_badge_supply);
            binding.tvMissionTitle.setText(R.string.volunteer_sos_acceptance_mission_title_supply);
            binding.cardSupplyItems.setVisibility(View.VISIBLE);
            return;
        }

        binding.cardMissionBadge.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.sos_red));
        binding.tvMissionBadge.setText(R.string.volunteer_sos_acceptance_mission_badge);
        binding.tvMissionTitle.setText(R.string.volunteer_sos_acceptance_mission_title);
        binding.cardSupplyItems.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        binding.btnDecline.setOnClickListener(v -> {
            volunteerTaskViewModel.declineMission();
            showToast(getString(R.string.volunteer_sos_acceptance_toast_decline_success));
            popBackStackSafely();
        });

        binding.btnAccept.setOnClickListener(v -> {
            if (MISSION_TYPE_SUPPLY.equalsIgnoreCase(missionType)) {
                volunteerTaskViewModel.acceptMission(MOCK_SUPPLY_MISSION_ID, MISSION_TYPE_SUPPLY);
                showToast(getString(R.string.volunteer_sos_acceptance_toast_accept_success));
                navigateSafely(R.id.action_sos_acceptance_to_delivery_mission);
                return;
            }

            volunteerTaskViewModel.acceptMission(MOCK_MISSION_ID, MISSION_TYPE_RESCUE);
            showToast(getString(R.string.volunteer_sos_acceptance_toast_accept_success));
            navigateSafely(R.id.action_sos_acceptance_to_current_sos_mission);
        });
    }

    private void startCountDownTimer() {
        cancelCountDownTimer();

        missionCountDownTimer = new CountDownTimer(TIMER_TOTAL_MS, TIMER_INTERVAL_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000L;
                binding.tvTimerSecondsValue.setText(String.format("%02d", secondsLeft));
            }

            @Override
            public void onFinish() {
                binding.tvTimerSecondsValue.setText(R.string.volunteer_sos_acceptance_timer_minutes_value);
                popBackStackSafely();
            }
        };

        missionCountDownTimer.start();
    }

    private void cancelCountDownTimer() {
        if (missionCountDownTimer != null) {
            missionCountDownTimer.cancel();
            missionCountDownTimer = null;
        }
    }
}