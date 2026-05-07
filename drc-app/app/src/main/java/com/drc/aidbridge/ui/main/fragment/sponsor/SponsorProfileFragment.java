package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentSponsorProfileBinding;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.MainActivity;
import com.drc.aidbridge.ui.main.viewmodel.sponsor.SponsorProfileViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorProfileFragment extends BaseFragment<FragmentSponsorProfileBinding> {

    private SponsorProfileViewModel viewModel;

    @Nullable
    @Override
    protected FragmentSponsorProfileBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(SponsorProfileViewModel.class);

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

        binding.btnLogout.setOnClickListener(v -> requestLogout());
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUserLiveData().observe(getViewLifecycleOwner(),
            resultObserver(this::bindUserProfile, this::showLoadError));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadProfile();
        }
    }

    private void bindUserProfile(@Nullable User user) {
        if (user == null) {
            return;
        }

        binding.tvSponsorName.setText(
            safeOrFallback(user.getName(), getString(R.string.sponsor_profile_mock_name)));

        String avatarUrl = trimToNull(user.getAvatarUrl());
        if (avatarUrl != null) {
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)
                .into(binding.ivAvatar);
        } else {
            binding.ivAvatar.setImageResource(R.drawable.ic_avatar);
        }
    }

    private void showLoadError(String message) {
        showTopSnackbar(binding.getRoot(), message, true);
    }

    private String safeOrFallback(@Nullable String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed != null ? trimmed : fallback;
    }

    @Nullable
    private String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void requestLogout() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).requestLogout();
        }
    }
}
