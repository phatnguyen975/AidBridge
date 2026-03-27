package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentSponsorProfileBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorProfileFragment extends BaseFragment<FragmentSponsorProfileBinding> {

    @Nullable
    @Override
    protected FragmentSponsorProfileBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        binding.layoutAvatar.setOnClickListener(v ->
            showToast("Mở thư viện ảnh..."));
        binding.cardEditAvatar.setOnClickListener(v ->
            showToast("Mở thư viện ảnh..."));

        binding.rowEditProfile.setOnClickListener(v ->
            navigateToDestinationSafely(R.id.sponsorEditProfileFragment));
        binding.rowChangePassword.setOnClickListener(v ->
            navigateToDestinationSafely(R.id.sponsorChangePasswordFragment));
        binding.rowDonationHistory.setOnClickListener(v ->
            navigateToDestinationSafely(R.id.sponsorHistoryFragment));

        binding.btnLogout.setOnClickListener(v ->
            showToast("Đã đăng xuất"));
    }

    @Override
    protected void observeViewModel() {
    }
}
