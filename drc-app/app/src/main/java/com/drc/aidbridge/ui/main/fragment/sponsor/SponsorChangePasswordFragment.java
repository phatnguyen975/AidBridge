package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentSponsorChangePasswordBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorChangePasswordFragment extends BaseFragment<FragmentSponsorChangePasswordBinding> {

    @Nullable
    @Override
    protected FragmentSponsorChangePasswordBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorChangePasswordBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        binding.ivBack.setOnClickListener(v -> popBackStackSafely());

        binding.btnUpdatePassword.setOnClickListener(v -> {
            showToast("Cập nhật mật khẩu thành công");
            popBackStackSafely();
        });
    }

    @Override
    protected void observeViewModel() {
        // TODO: Add ViewModel observers when change-password API is integrated.
    }
}
