package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.databinding.FragmentVolunteerDashboardBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerDashboardViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerDashboardFragment extends BaseFragment<FragmentVolunteerDashboardBinding> {

    private VolunteerDashboardViewModel viewModel;

    @Override
    protected FragmentVolunteerDashboardBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentVolunteerDashboardBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        // Online Status (Online by default - Test UI)
        updateStatusUI(true);
        binding.switchOnlineStatus.setChecked(true);

        setupClickListeners();
    }

    @Override
    protected void observeViewModel() {
        // TODO: Observe ViewModel state once use cases are implemented
    }

    private void setupClickListeners() {
        binding.switchOnlineStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateStatusUI(isChecked);
            showToast(getString(isChecked
                    ? com.drc.aidbridge.R.string.volunteer_dashboard_toast_mode_ready
                    : com.drc.aidbridge.R.string.volunteer_dashboard_toast_mode_offline));
        });

        binding.cardUserInfo.setOnClickListener(v -> showToast(getString(com.drc.aidbridge.R.string.volunteer_dashboard_toast_view_profile)));

        binding.cardCurrentMission.setOnClickListener(v -> showToast(getString(com.drc.aidbridge.R.string.volunteer_dashboard_toast_open_current_missions)));

        binding.cardCompleted.setOnClickListener(v -> showToast(getString(com.drc.aidbridge.R.string.volunteer_dashboard_toast_open_completed_missions)));

        binding.tvSeeAll.setOnClickListener(v -> showToast(getString(com.drc.aidbridge.R.string.volunteer_dashboard_toast_see_all_notifications)));

        binding.btnDetails.setOnClickListener(v -> showToast(getString(com.drc.aidbridge.R.string.volunteer_dashboard_toast_view_mission_detail)));

        binding.btnLogout.setOnClickListener(v -> showToast(getString(com.drc.aidbridge.R.string.volunteer_dashboard_toast_logout)));
    }

    private void updateStatusUI(boolean isOnline) {
        if (isOnline) {
            binding.tvStatusDescription.setText(com.drc.aidbridge.R.string.volunteer_dashboard_status_online_placeholder);
            binding.viewStatusIndicator.setBackgroundResource(com.drc.aidbridge.R.drawable.bg_circle_status_online);
            binding.btnReady.setAlpha(1.0f);
            binding.btnOffline.setAlpha(0.5f);
        } else {
            binding.tvStatusDescription.setText(com.drc.aidbridge.R.string.volunteer_dashboard_status_offline_placeholder);
            binding.viewStatusIndicator.setBackgroundResource(com.drc.aidbridge.R.drawable.bg_circle_status_offline);
            binding.btnReady.setAlpha(0.5f);
            binding.btnOffline.setAlpha(1.0f);
        }
    }
}
