package com.drc.aidbridge.ui.auth.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentForgotNewPasswordBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.auth.viewmodel.ForgotNewPasswordViewModel;

import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * ForgotNewPasswordFragment — Forgot Password: Step 3 — Set New Password.
 *
 * The user enters and confirms a new password.
 * Client-side validation (length, match) runs before delegating to the ViewModel.
 * On success: shows a confirmation Snackbar and pops back to LoginFragment,
 * clearing the entire forgot-password flow from the back stack.
 *
 * API: POST /auth/reset-password  { "email": "...", "newPassword": "..." }
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

    // ------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> popBackStackSafely());

        binding.btnChangePassword.setOnClickListener(v -> attemptChangePassword());
    }

    @Override
    protected void observeViewModel() {
        viewModel.getChangePasswordResult().observe(getViewLifecycleOwner(),
                resultObserver(binding.btnChangePassword,
                        ignored -> showSuccessAndReturnToLogin()));
    }

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    @Override
    protected ProgressBar getLoadingView() {
        return binding != null ? binding.progressBar : null;
    }

    // ------------------------------------------------------------------
    // Logic
    // ------------------------------------------------------------------

    /**
     * Runs client-side validation (empty, length, match) and delegates to the ViewModel.
     * Only calls viewModel.changePassword() if all checks pass — the ViewModel
     * assumes the input is already valid.
     */
    private void attemptChangePassword() {
        binding.tilNewPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        String newPwd     = getText(binding.tilNewPassword);
        String confirmPwd = getText(binding.tilConfirmPassword);

        boolean valid = true;
        if (TextUtils.isEmpty(newPwd)) {
            binding.tilNewPassword.setError(getString(R.string.error_field_empty));
            valid = false;
        } else if (newPwd.length() < 6) {
            binding.tilNewPassword.setError(getString(R.string.error_password_short));
            valid = false;
        }
        if (!newPwd.equals(confirmPwd)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            valid = false;
        }
        if (!valid) return;

        viewModel.changePassword(newPwd);
    }

    // ------------------------------------------------------------------
    // Success flow
    // ------------------------------------------------------------------

    /**
     * Shows a success Snackbar and pops the entire forgot-password flow back to LoginFragment.
     */
    private void showSuccessAndReturnToLogin() {
        Snackbar.make(binding.getRoot(),
                getString(R.string.forgot_success_message),
                Snackbar.LENGTH_SHORT)
                .setBackgroundTint(requireContext().getColor(R.color.safe_green))
                .setTextColor(requireContext().getColor(R.color.white))
                .show();

        popBackStackSafely(R.id.loginFragment, false);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String getText(com.google.android.material.textfield.TextInputLayout til) {
        if (til.getEditText() == null) return "";
        CharSequence text = til.getEditText().getText();
        return text != null ? text.toString().trim() : "";
    }
}
