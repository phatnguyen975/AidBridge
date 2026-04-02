package com.drc.aidbridge.ui.auth.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentForgotNewPasswordBinding;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.auth.viewmodel.ForgotNewPasswordViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * ForgotNewPasswordFragment — Forgot Password: Step 3 — Set New Password.
 */
@AndroidEntryPoint
public class ForgotNewPasswordFragment extends BaseFragment<FragmentForgotNewPasswordBinding> {
    private ForgotNewPasswordViewModel viewModel;

    @Override
    protected FragmentForgotNewPasswordBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentForgotNewPasswordBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(ForgotNewPasswordViewModel.class);
        setupClickListeners();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getValidationError().observe(getViewLifecycleOwner(), validation -> {
            clearErrors();

            if (validation == null || validation.isValid()) {
                return;
            }

            if (validation.getErrorField() == ValidationResult.Field.PASSWORD) {
                binding.tilNewPassword.setError(validation.getErrorMessage());
                binding.tilNewPassword.requestFocus();
            } else if (validation.getErrorField() == ValidationResult.Field.CONFIRM_PASSWORD) {
                binding.tilConfirmPassword.setError(validation.getErrorMessage());
                binding.tilConfirmPassword.requestFocus();
            } else if (validation.getErrorField() == ValidationResult.Field.EMAIL) {
                showTopSnackbar(binding.getRoot(), validation.getErrorMessage(), true);
            } else {
                showTopSnackbar(binding.getRoot(), validation.getErrorMessage(), true);
            }
        });

        viewModel.getChangePasswordResult().observe(getViewLifecycleOwner(),
                resultObserver(
                        ignored -> showSuccessAndReturnToLogin(),
                        this::showNetworkError));
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        applyActionLoadingState(
            binding.btnChangePassword,
            binding.progressChangePasswordInline,
            binding.tvChangePasswordButtonText,
            isLoading,
            R.string.forgot_btn_change_password
        );
    }

    private void setupClickListeners() {
        binding.btnChangePassword.setOnClickListener(v -> attemptChangePassword());
    }

    private void attemptChangePassword() {
        clearInputFocusAndHideKeyboard();
        String newPwd = getRawText(binding.tilNewPassword.getEditText());
        String confirmPwd = getRawText(binding.tilConfirmPassword.getEditText());
        viewModel.changePassword(newPwd, confirmPwd);
    }

    private String getRawText(EditText et) {
        if (et == null) {
            return "";
        }
        return et.getText() != null ? et.getText().toString() : "";
    }

    private void showNetworkError(String message) {
        showTopSnackbar(binding.getRoot(), message, true);
    }

    private void clearErrors() {
        binding.tilNewPassword.setError(null);
        binding.tilConfirmPassword.setError(null);
    }

    private void showSuccessAndReturnToLogin() {
        showTopSnackbar(binding.getRoot(), getString(R.string.forgot_success_message), false);
        navigateSafely(R.id.action_forgotNewPasswordFragment_to_loginFragment);
    }
}
