package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerDashboardBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerTaskViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerDashboardFragment extends BaseFragment<FragmentVolunteerDashboardBinding> {

    private VolunteerTaskViewModel volunteerTaskViewModel;
    private boolean isMissionAccepted;
    private boolean isMissionIgnored;

    @Override
    protected FragmentVolunteerDashboardBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentVolunteerDashboardBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        volunteerTaskViewModel = new ViewModelProvider(requireActivity()).get(VolunteerTaskViewModel.class);

        // Online Status (Online by default - Test UI)
        updateStatusUI(true);
        binding.switchOnlineStatus.setChecked(true);

        setupClickListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isAdded() || getActivity() == null) {
            return;
        }

        BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            MenuItem dashboardItem = bottomNav.getMenu().findItem(R.id.volunteerDashboardFragment);
            if (dashboardItem != null) {
                dashboardItem.setChecked(true);
            }
        }
    }

    @Override
    protected void observeViewModel() {
        if (volunteerTaskViewModel == null) {
            return;
        }

        volunteerTaskViewModel.getIsMissionAccepted().observe(getViewLifecycleOwner(), isAccepted -> {
            isMissionAccepted = Boolean.TRUE.equals(isAccepted);
            renderMissionCardsState();
        });

        volunteerTaskViewModel.getIsMissionIgnored().observe(getViewLifecycleOwner(), isIgnored -> {
            isMissionIgnored = Boolean.TRUE.equals(isIgnored);
            renderMissionCardsState();
        });
    }

    private void renderMissionCardsState() {
        boolean shouldShowEmergencyCard = !isMissionAccepted && !isMissionIgnored;
        binding.cardEmergency.setVisibility(shouldShowEmergencyCard ? View.VISIBLE : View.GONE);
        binding.cardCurrentMission.setVisibility(isMissionAccepted ? View.VISIBLE : View.GONE);
    }

    private void setupClickListeners() {
        String mockNotiType = "RESCUE";

        binding.switchOnlineStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateStatusUI(isChecked);
            showToast(getString(isChecked
                    ? com.drc.aidbridge.R.string.volunteer_dashboard_toast_mode_ready
                    : com.drc.aidbridge.R.string.volunteer_dashboard_toast_mode_offline));
        });

        binding.cardUserInfo.setOnClickListener(
                v -> showToast(getString(com.drc.aidbridge.R.string.volunteer_dashboard_toast_view_profile)));

        binding.cardCurrentMission.setOnClickListener(
                v -> navigateSafely(R.id.action_dashboard_to_current_sos_mission));

        // binding.cardCompleted.setOnClickListener(v ->
        // showToast(getString(com.drc.aidbridge.R.string.volunteer_dashboard_toast_open_completed_missions)));
        binding.cardCompleted.setOnClickListener(v -> navigateSafely(R.id.action_dashboard_to_history));

        binding.tvSeeAll.setOnClickListener(
                v -> showToast(getString(com.drc.aidbridge.R.string.volunteer_dashboard_toast_see_all_notifications)));

        binding.btnDetails.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("missionType", mockNotiType);
            navigateSafely(R.id.action_dashboard_to_sos_acceptance, bundle);
        });

        binding.btnLogout.setOnClickListener(
                v -> showToast(getString(com.drc.aidbridge.R.string.volunteer_dashboard_toast_logout)));
    }

    private void updateStatusUI(boolean isOnline) {
        if (isOnline) {
            binding.tvStatusDescription
                    .setText(com.drc.aidbridge.R.string.volunteer_dashboard_status_online_placeholder);
            binding.viewStatusIndicator.setBackgroundResource(com.drc.aidbridge.R.drawable.bg_circle_status_online);
            binding.btnReady.setAlpha(1.0f);
            binding.btnOffline.setAlpha(0.5f);
        } else {
            binding.tvStatusDescription
                    .setText(com.drc.aidbridge.R.string.volunteer_dashboard_status_offline_placeholder);
            binding.viewStatusIndicator.setBackgroundResource(com.drc.aidbridge.R.drawable.bg_circle_status_offline);
            binding.btnReady.setAlpha(0.5f);
            binding.btnOffline.setAlpha(1.0f);
        }
    }
}
