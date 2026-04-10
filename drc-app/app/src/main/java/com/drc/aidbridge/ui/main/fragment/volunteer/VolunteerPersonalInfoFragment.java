package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.databinding.FragmentVolunteerPersonalInfoBinding;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerPersonalInfoViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerPersonalInfoFragment extends BaseFragment<FragmentVolunteerPersonalInfoBinding> {

	private VolunteerPersonalInfoViewModel volunteerPersonalInfoViewModel;

	@Nullable
	@Override
	protected FragmentVolunteerPersonalInfoBinding inflateBinding(LayoutInflater inflater,
			@Nullable ViewGroup container) {
		return FragmentVolunteerPersonalInfoBinding.inflate(inflater, container, false);
	}

	@Override
	protected void setupViews() {
		volunteerPersonalInfoViewModel = new ViewModelProvider(this).get(VolunteerPersonalInfoViewModel.class);
		setupClickListeners();
		volunteerPersonalInfoViewModel.loadUserInfo();
	}

	@Override
	protected void observeViewModel() {
		if (volunteerPersonalInfoViewModel == null) {
			return;
		}

		volunteerPersonalInfoViewModel.getValidationError().observe(getViewLifecycleOwner(),
				this::renderValidationError);

		volunteerPersonalInfoViewModel.getUserInfoResult().observe(
				getViewLifecycleOwner(),
				resultObserver(this::renderPersonalInfoFromUser, this::showUpdateError));

		volunteerPersonalInfoViewModel.getUpdateProfileResult().observe(
				getViewLifecycleOwner(),
				result -> {
					renderUpdateInfoLoading(result != null && result.isLoading());
					resultObserver(this::handleUpdateProfileSuccess, this::showUpdateError).onChanged(result);
				});

		volunteerPersonalInfoViewModel.getChangePasswordResult().observe(
				getViewLifecycleOwner(),
				result -> {
					renderChangePasswordLoading(result != null && result.isLoading());
					resultObserver(this::handleChangePasswordSuccess, this::showChangePasswordError).onChanged(result);
				});
	}

	private void renderPersonalInfoFromUser(@Nullable User user) {
		if (user == null) {
			return;
		}

		String fullName = user.getName() != null && !user.getName().trim().isEmpty()
				? user.getName().trim()
				: getString(R.string.volunteer_personal_info_default_full_name);
		String phoneNumber = user.getPhone() != null && !user.getPhone().trim().isEmpty()
				? user.getPhone().trim()
				: getString(R.string.volunteer_personal_info_default_phone);
		String email = user.getEmail() != null && !user.getEmail().trim().isEmpty()
				? user.getEmail().trim()
				: getString(R.string.volunteer_personal_info_default_email);

		binding.etFullName.setText(fullName);
		binding.etPhone.setText(phoneNumber);
		binding.etEmail.setText(email);
	}

	private void renderValidationError(@Nullable ValidationResult validationResult) {
		if (validationResult == null || validationResult.isValid()) {
			return;
		}

		String message = validationResult.getErrorMessage() != null
				? validationResult.getErrorMessage()
				: getString(R.string.error_generic);
		showTopSnackbar(binding.getRoot(), message, true);
	}

	private void handleUpdateProfileSuccess(@Nullable User user) {
		renderPersonalInfoFromUser(user);
		showTopSnackbar(
				binding.getRoot(),
				getString(R.string.volunteer_personal_info_toast_update_success),
				false);
	}

	private void showUpdateError(@NonNull String message) {
		showTopSnackbar(binding.getRoot(), message, true);
	}

	private void handleChangePasswordSuccess(@Nullable String message) {
		binding.etCurrentPassword.setText("");
		binding.etNewPassword.setText("");
		binding.etConfirmNewPassword.setText("");

		String safeMessage = message != null && !message.trim().isEmpty()
				? message
				: getString(R.string.volunteer_personal_info_toast_change_password_success);
		showTopSnackbar(binding.getRoot(), safeMessage, false);
	}

	private void showChangePasswordError(@NonNull String message) {
		showTopSnackbar(binding.getRoot(), message, true);
	}

	private void setupClickListeners() {
		binding.btnBack.setOnClickListener(v -> popBackStackSafely());

		binding.btnUpdateInfo.setOnClickListener(v -> {
			clearInputFocusAndHideKeyboard();
			volunteerPersonalInfoViewModel.updateProfile(
					getRawText(binding.etFullName),
					getRawText(binding.etPhone),
					"");
		});

		binding.btnChangePassword.setOnClickListener(v -> {
			clearInputFocusAndHideKeyboard();

			String currentPassword = getRawText(binding.etCurrentPassword);
			String newPassword = String.valueOf(binding.etNewPassword.getText()).trim();
			String confirmPassword = String.valueOf(binding.etConfirmNewPassword.getText()).trim();

			volunteerPersonalInfoViewModel.changePassword(currentPassword, newPassword, confirmPassword);
		});
	}

	private String getRawText(@Nullable EditText editText) {
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
				R.string.volunteer_personal_info_btn_update);
	}

	private void renderChangePasswordLoading(boolean isLoading) {
		applyActionLoadingState(
				binding.btnChangePassword,
				binding.progressChangePasswordInline,
				binding.tvChangePasswordButtonText,
				isLoading,
				R.string.volunteer_personal_info_btn_change_password);
	}
}
