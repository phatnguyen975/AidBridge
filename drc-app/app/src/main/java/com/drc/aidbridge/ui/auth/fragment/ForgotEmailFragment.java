package com.drc.aidbridge.ui.auth.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentForgotEmailBinding;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.auth.viewmodel.ForgotEmailViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * ForgotEmailFragment â€” Forgot Password: Step 1 â€” Enter Email.
 */
@AndroidEntryPoint
public class ForgotEmailFragment extends BaseFragment<FragmentForgotEmailBinding> {
    private ForgotEmailViewModel viewModel;
    private String submittedEmail = "";

    @Override
    protected FragmentForgotEmailBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentForgotEmailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(ForgotEmailViewModel.class);
        setupClickListeners();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getValidationError().observe(getViewLifecycleOwner(), validation -> {
            clearErrors();

            if (validation == null || validation.isValid()) {
                return;
            }

            if (validation.getErrorField() == ValidationResult.Field.EMAIL) {
                binding.tilEmail.setError(validation.getErrorMessage());
                binding.tilEmail.requestFocus();
            } else {
                showTopSnackbar(binding.getRoot(), validation.getErrorMessage(), true);
            }
        });

        viewModel.getSendOtpResult().observe(getViewLifecycleOwner(),
            resultObserver(this::handleSendOtpSuccess, this::showNetworkError));
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        applyActionLoadingState(
            binding.btnSendOtp,
            binding.progressSendOtpInline,
            binding.tvSendOtpButtonText,
            isLoading,
            R.string.forgot_btn_send_otp
        );
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> popBackStackSafely());
        binding.btnSendOtp.setOnClickListener(v -> attemptSendOtp());
    }

    private void attemptSendOtp() {
        clearInputFocusAndHideKeyboard();
        submittedEmail = getRawText(binding.tilEmail.getEditText()).trim().toLowerCase();
        viewModel.sendOtp(submittedEmail);
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
        binding.tilEmail.setError(null);
    }

    private void navigateToOtp(String email) {
        Bundle args = new Bundle();
        args.putString("email", email);
        navigateSafely(R.id.action_forgotEmailFragment_to_forgotOtpFragment, args);
    }

    private void handleSendOtpSuccess(String apiMessage) {
        String successMessage = (apiMessage != null && !apiMessage.trim().isEmpty())
            ? apiMessage
            : getString(R.string.otp_resend_success);
        showTopSnackbar(binding.getRoot(), successMessage, false);
        navigateToOtp(submittedEmail);
    }
}

