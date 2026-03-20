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
        viewModel = new ViewModelProvider(this).get(VolunteerDashboardViewModel.class);
        
        binding.switchOnlineStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.toggleOnlineStatus(isChecked);
        });

        binding.btnLogout.setOnClickListener(v -> {
            // Logic đăng xuất
            showToast("Đang đăng xuất...");
        });
    }

    @Override
    protected void observeViewModel() {
        viewModel.isOnline().observe(getViewLifecycleOwner(), isOnline -> {
            updateStatusUI(isOnline);
        });

        viewModel.getUpdateStatusResult().observe(getViewLifecycleOwner(), 
            resultObserver(binding.switchOnlineStatus, isOnline -> {
                // Thành công
            })
        );
    }

    private void updateStatusUI(boolean isOnline) {
        if (isOnline) {
            binding.tvStatusDescription.setText("Sẵn sàng nhận nhiệm vụ mới");
            binding.viewStatusIndicator.setBackgroundResource(com.drc.aidbridge.R.drawable.bg_circle_status_online);
            binding.btnReady.setAlpha(1.0f);
            binding.btnOffline.setAlpha(0.5f);
        } else {
            binding.tvStatusDescription.setText("Bạn đang ngoại tuyến");
            binding.viewStatusIndicator.setBackgroundResource(com.drc.aidbridge.R.drawable.bg_circle_status_offline);
            binding.btnReady.setAlpha(0.5f);
            binding.btnOffline.setAlpha(1.0f);
        }
    }
}
