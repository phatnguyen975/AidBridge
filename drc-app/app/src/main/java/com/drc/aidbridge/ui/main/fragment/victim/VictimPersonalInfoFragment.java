package com.drc.aidbridge.ui.main.fragment.victim;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.FragmentVictimPersonalInfoBinding;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.usecase.validation.AuthValidationResult;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.victim.VictimPersonalInfoViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimPersonalInfoFragment extends BaseFragment<FragmentVictimPersonalInfoBinding> {

    private VictimPersonalInfoViewModel viewModel;
    private String currentAvatarUrl;

    private final ActivityResultLauncher<String> imagePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleAvatarImageSelected);

    @Nullable
    @Override
    protected FragmentVictimPersonalInfoBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimPersonalInfoBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(VictimPersonalInfoViewModel.class);

        binding.btnBack.setOnClickListener(v -> popBackStackSafely());
        binding.ivEditAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        binding.btnUpdateInfo.setOnClickListener(v -> onUpdateProfileClicked());
        binding.btnChangePassword.setOnClickListener(v -> onChangePasswordClicked());

        viewModel.loadUserInfo();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getValidationError().observe(getViewLifecycleOwner(), this::renderValidationError);

        viewModel.getUserInfoResult().observe(getViewLifecycleOwner(),
            resultObserver(this::bindUserInfo, this::showScreenError));

        viewModel.getUpdateProfileResult().observe(getViewLifecycleOwner(), this::handleUpdateProfileResult);
        viewModel.getChangePasswordResult().observe(getViewLifecycleOwner(), this::handleChangePasswordResult);
        viewModel.getUploadAvatarResult().observe(getViewLifecycleOwner(), this::handleUploadAvatarResult);
    }

    private void onUpdateProfileClicked() {
        clearInputFocusAndHideKeyboard();
        viewModel.updateProfile(
            getRawText(binding.etFullName),
            getRawText(binding.etPhone),
            getRawText(binding.etAddress)
        );
    }

    private void onChangePasswordClicked() {
        clearInputFocusAndHideKeyboard();

        String currentPassword = getRawText(binding.etCurrentPassword);
        String newPassword = getRawText(binding.etNewPassword);
        String confirmPassword = getRawText(binding.etConfirmNewPassword);

        viewModel.changePassword(currentPassword, newPassword, confirmPassword);
    }

    private void renderValidationError(@Nullable AuthValidationResult validationResult) {
        if (validationResult == null || validationResult.isValid()) {
            return;
        }

        String message = validationResult.getErrorMessage();
        showTopSnackbar(
            binding.getRoot(),
            message != null && !message.trim().isEmpty()
                ? message
                : getString(R.string.error_generic),
            true
        );
    }

    private void bindUserInfo(@Nullable User user) {
        if (user == null) {
            return;
        }

        binding.etFullName.setText(safeTrim(user.getName()));
        binding.etPhone.setText(safeTrim(user.getPhone()));
        binding.etEmail.setText(user.getEmail() != null ? user.getEmail().trim() : "");
        binding.etAddress.setText(user.getAddress() != null ? user.getAddress().trim() : "");

        currentAvatarUrl = trimToNull(user.getAvatarUrl());
        renderAvatar(currentAvatarUrl);
    }

    private void showScreenError(String message) {
        showTopSnackbar(binding.getRoot(), message, true);
    }

    private void handleUpdateProfileResult(NetworkResultWrapper<User> result) {
        if (result == null) {
            return;
        }

        renderUpdateInfoLoading(result.isLoading());

        if (result.isLoading() || result.hasBeenHandled()) {
            return;
        }

        result.markAsHandled();
        if (result.isSuccess()) {
            bindUserInfo(result.getData());
            showTopSnackbar(binding.getRoot(),
                getString(R.string.victim_personal_info_message_update_success),
                false);
            return;
        }

        if (result.isError()) {
            showTopSnackbar(binding.getRoot(), result.getMessage(), true);
        }
    }

    private void handleChangePasswordResult(NetworkResultWrapper<String> result) {
        if (result == null) {
            return;
        }

        renderChangePasswordLoading(result.isLoading());

        if (result.isLoading() || result.hasBeenHandled()) {
            return;
        }

        result.markAsHandled();
        if (result.isSuccess()) {
            binding.etCurrentPassword.setText("");
            binding.etNewPassword.setText("");
            binding.etConfirmNewPassword.setText("");

            String message = result.getData();
            showTopSnackbar(binding.getRoot(),
                message != null && !message.trim().isEmpty()
                    ? message
                    : getString(R.string.victim_personal_info_message_change_password_success),
                false);
            return;
        }

        if (result.isError()) {
            showTopSnackbar(binding.getRoot(), result.getMessage(), true);
        }
    }

    private void handleUploadAvatarResult(NetworkResultWrapper<String> result) {
        if (result == null) {
            return;
        }

        binding.ivEditAvatar.setEnabled(!result.isLoading());

        if (result.isLoading() || result.hasBeenHandled()) {
            return;
        }

        result.markAsHandled();
        if (result.isSuccess()) {
            String avatarUrl = trimToNull(result.getData());
            if (avatarUrl != null) {
                currentAvatarUrl = avatarUrl;
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

    private void handleAvatarImageSelected(@Nullable Uri selectedUri) {
        if (selectedUri == null) {
            return;
        }

        renderAvatar(selectedUri);
        viewModel.uploadAvatar(requireContext(), selectedUri);
    }

    private String getRawText(EditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private void renderUpdateInfoLoading(boolean isLoading) {
        applyActionLoadingState(
            binding.btnUpdateInfo,
            binding.progressUpdateInfoInline,
            binding.tvUpdateInfoButtonText,
            isLoading,
            R.string.victim_personal_info_btn_update
        );
    }

    private void renderChangePasswordLoading(boolean isLoading) {
        applyActionLoadingState(
            binding.btnChangePassword,
            binding.progressChangePasswordInline,
            binding.tvChangePasswordButtonText,
            isLoading,
            R.string.victim_personal_info_btn_change_password
        );
    }

    private void renderAvatar(@Nullable Object avatarSource) {
        Glide.with(this)
            .load(avatarSource)
            .placeholder(R.drawable.ic_avatar)
            .error(R.drawable.ic_avatar)
            .into(binding.ivAvatar);
    }

    private String safeTrim(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
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

