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
            showToast(isChecked ? "Chế độ: Sẵn sàng" : "Chế độ: Ngoại tuyến");
        });

        binding.cardUserInfo.setOnClickListener(v -> showToast("Xem hồ sơ cá nhân"));

        binding.cardCurrentMission.setOnClickListener(v -> showToast("Mở danh sách nhiệm vụ hiện tại"));

        binding.cardCompleted.setOnClickListener(v -> showToast("Xem lịch sử nhiệm vụ đã hoàn thành"));

        binding.tvSeeAll.setOnClickListener(v -> showToast("Xem tất cả thông báo"));

        binding.btnDetails.setOnClickListener(v -> showToast("Xem chi tiết nhiệm vụ cứu hộ"));

        binding.btnLogout.setOnClickListener(v -> showToast("Xử lý đăng xuất"));
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
