package com.drc.aidbridge.ui.main.fragment.victim;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.FragmentVictimProfileBinding;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.MainActivity;
import com.drc.aidbridge.ui.main.viewmodel.victim.VictimProfileViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimProfileFragment extends BaseFragment<FragmentVictimProfileBinding> {

    private VictimProfileViewModel viewModel;
    private String currentAvatarUrl;

    private final ActivityResultLauncher<String> imagePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleAvatarImageSelected);

    @Nullable
    @Override
    protected FragmentVictimProfileBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(VictimProfileViewModel.class);
        setupClickListeners();
        viewModel.loadUserInfo();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadUserInfo();
        }
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUserInfoResult().observe(getViewLifecycleOwner(),
            resultObserver(this::bindUserInfo, this::showLoadError));

        viewModel.getUploadAvatarResult().observe(getViewLifecycleOwner(), this::handleUploadAvatarResult);
    }

    private void setupClickListeners() {
        binding.cardTrackJourney.setOnClickListener(v ->
            navigateSafely(R.id.action_profile_to_map));

        binding.cardRequestHistory.setOnClickListener(v ->
            navigateSafely(R.id.action_profile_to_history));

        binding.rowPersonalInfo.setOnClickListener(v ->
            navigateSafely(R.id.action_profile_to_personalInfo));

        binding.rowLogout.setOnClickListener(v -> requestLogout());
        binding.ivEditAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
    }

    private void requestLogout() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).requestLogout();
        }
    }

    private void bindUserInfo(@Nullable User user) {
        if (user == null) {
            return;
        }

        String displayName = safeOrFallback(user.getName(), getString(R.string.victim_profile_name_placeholder));
        String displayPhone = safeOrFallback(user.getPhone(), getString(R.string.victim_profile_phone_placeholder));

        binding.tvVictimName.setText(displayName);
        binding.tvVictimPhone.setText(displayPhone);

        currentAvatarUrl = trimToNull(user.getAvatarUrl());
        renderAvatar(currentAvatarUrl);
    }

    private void handleAvatarImageSelected(@Nullable Uri selectedUri) {
        if (selectedUri == null) {
            return;
        }

        renderAvatar(selectedUri);
        viewModel.uploadAvatar(requireContext(), selectedUri);
    }

    private void handleUploadAvatarResult(NetworkResultWrapper<String> result) {
        if (result == null) {
            return;
        }

        boolean isLoading = result.isLoading();
        binding.ivEditAvatar.setEnabled(!isLoading);

        if (isLoading || result.hasBeenHandled()) {
            return;
        }

        result.markAsHandled();
        if (result.isSuccess()) {
            String uploadedAvatarUrl = trimToNull(result.getData());
            if (uploadedAvatarUrl != null) {
                currentAvatarUrl = uploadedAvatarUrl;
            }
            renderAvatar(currentAvatarUrl);
            showTopSnackbar(binding.getRoot(),
                getString(R.string.victim_personal_info_message_avatar_updated),
                false);
            return;
        }

        if (result.isError()) {
            renderAvatar(currentAvatarUrl);
            showTopSnackbar(binding.getRoot(), result.getMessage(), true);
        }
    }

    private void showLoadError(String message) {
        showTopSnackbar(binding.getRoot(), message, true);
    }

    private String safeOrFallback(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private void renderAvatar(@Nullable Object avatarSource) {
        Glide.with(this)
            .load(avatarSource)
            .placeholder(R.drawable.ic_avatar)
            .error(R.drawable.ic_avatar)
            .into(binding.ivAvatar);
    }

    @Nullable
    private String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
