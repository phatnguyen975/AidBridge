package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentSponsorEditProfileBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorEditProfileFragment extends BaseFragment<FragmentSponsorEditProfileBinding> {

    @Nullable
    @Override
    protected FragmentSponsorEditProfileBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorEditProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        binding.ivBack.setOnClickListener(v -> popBackStackSafely());

        binding.btnUpdateProfile.setOnClickListener(v -> {
            showToast("Cập nhật thông tin thành công");
            popBackStackSafely();
        });
    }

    @Override
    protected void observeViewModel() {
        // TODO: Add ViewModel observers when profile update API is integrated.
    }
}
