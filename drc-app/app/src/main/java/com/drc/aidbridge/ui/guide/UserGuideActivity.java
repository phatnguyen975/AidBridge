package com.drc.aidbridge.ui.guide;

import android.view.LayoutInflater;

import com.drc.aidbridge.databinding.ActivityUserGuideBinding;
import com.drc.aidbridge.ui.base.BaseActivity;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * UserGuideActivity — Hướng dẫn sử dụng ứng dụng AidBridge.
 *
 * Launched from GuestFragment when the user taps the Info (ⓘ) icon.
 * Content covers:
 *   1. Tính năng SOS Khẩn cấp — cách nhấn nút SOS, điền thông tin, gửi GPS
 *   2. Xem bản đồ cứu trợ — Hub markers, Shelter markers, Safe paths, Heatmap
 *   3. Đăng ký / Đăng nhập — tạo tài khoản, OTP, chọn vai trò
 *   4. Các vai trò người dùng — Nạn nhân, TNV, Mạnh thường quân
 *
 * Content is static (strings.xml); no API calls needed.
 */
@AndroidEntryPoint
public class UserGuideActivity extends BaseActivity<ActivityUserGuideBinding> {

    @Override
    protected ActivityUserGuideBinding inflateBinding(LayoutInflater inflater) {
        return ActivityUserGuideBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        setupClickListeners();
    }

    @Override
    protected void observeViewModel() {
    }

    private void setupClickListeners() {
        // Back arrow → finish this Activity and return to GuestFragment
        binding.btnBack.setOnClickListener(v -> finish());
    }
}
