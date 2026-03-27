package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerSosAcceptanceBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerSosAcceptanceFragment extends BaseFragment<FragmentVolunteerSosAcceptanceBinding> {

    public static final String ARG_MISSION_TYPE = "missionType";
    public static final String MISSION_TYPE_RESCUE = "RESCUE";
    public static final String MISSION_TYPE_SUPPLY = "SUPPLY";

    private static final long TIMER_TOTAL_MS = 60_000L;
    private static final long TIMER_INTERVAL_MS = 1_000L;

    private String missionType = MISSION_TYPE_RESCUE;
    private CountDownTimer missionCountDownTimer;

    @NonNull
    public static VolunteerSosAcceptanceFragment newInstance(@NonNull String missionType) {
        VolunteerSosAcceptanceFragment fragment = new VolunteerSosAcceptanceFragment();
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
    protected FragmentVolunteerSosAcceptanceBinding inflateBinding(LayoutInflater inflater,
            @Nullable ViewGroup container) {
        return FragmentVolunteerSosAcceptanceBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        applyMissionTypeUI();
        setupClickListeners();
        startCountDownTimer();
    }

    @Override
    protected void observeViewModel() {
        // TODO: Inject ViewModel và observe LiveData để cập nhật dữ liệu nhiệm vụ từ
        // UseCase
    }

    @Override
    public void onDestroyView() {
        cancelCountDownTimer();
        super.onDestroyView();
    }

    private void applyMissionTypeUI() {
        if (MISSION_TYPE_SUPPLY.equalsIgnoreCase(missionType)) {
            binding.cardMissionBadge.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.hub_blue));
            binding.tvMissionBadge.setText(R.string.volunteer_sos_acceptance_mission_badge_supply);
            binding.tvMissionTitle.setText(R.string.volunteer_sos_acceptance_mission_title_supply);
            return;
        }

        binding.cardMissionBadge.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.sos_red));
        binding.tvMissionBadge.setText(R.string.volunteer_sos_acceptance_mission_badge);
        binding.tvMissionTitle.setText(R.string.volunteer_sos_acceptance_mission_title);
    }

    private void setupClickListeners() {
        binding.btnDecline.setOnClickListener(v -> {
            showToast(getString(R.string.volunteer_sos_acceptance_toast_decline_success));
            popBackStackSafely();
        });

        binding.btnAccept.setOnClickListener(v -> {
            showToast(getString(R.string.volunteer_sos_acceptance_toast_accept_success));
            // TODO:
            // navigateSafely(R.id.action_volunteerSosAcceptanceFragment_to_volunteerMissionExecutionFragment)
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
