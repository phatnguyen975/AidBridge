package com.drc.aidbridge.ui.auth.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentForgotOtpBinding;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.common.OtpInputController;
import com.drc.aidbridge.ui.auth.viewmodel.ForgotOtpViewModel;

import java.util.Arrays;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * ForgotOtpFragment — Forgot Password: Step 2 — Enter OTP.
 */
@AndroidEntryPoint
public class ForgotOtpFragment extends BaseFragment<FragmentForgotOtpBinding> {

    private ForgotOtpViewModel viewModel;
    private OtpInputController otpInputController;

    @Override
    protected FragmentForgotOtpBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentForgotOtpBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(ForgotOtpViewModel.class);
        binding.tvEmail.setText(viewModel.getEmail());

        otpInputController = new OtpInputController(
            Arrays.asList(
                binding.etOtp1,
                binding.etOtp2,
                binding.etOtp3,
                binding.etOtp4,
                binding.etOtp5,
                binding.etOtp6
            ),
            null,
            otp -> attemptVerifyOtp()
        );
        otpInputController.bind();

        setupClickListeners();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getCountdown().observe(getViewLifecycleOwner(), seconds -> {
            if (seconds != null && seconds > 0) {
                String text = getString(R.string.otp_resend_countdown_template, seconds);
                binding.tvResend.setText(text);
                binding.tvResend.setTextColor(requireContext().getColor(R.color.text_secondary));
            }
        });

        viewModel.getResendEnabled().observe(getViewLifecycleOwner(), enabled -> {
            if (Boolean.TRUE.equals(enabled)) {
                binding.tvResend.setText(R.string.otp_resend_now);
                binding.tvResend.setTextColor(requireContext().getColor(R.color.text_link));
            }
        });

        viewModel.getValidationError().observe(getViewLifecycleOwner(), validation -> {
            if (validation == null || validation.isValid()) {
                return;
            }

            if (validation.getErrorField() == ValidationResult.Field.OTP) {
                clearOtpBoxes();
            } else {
                showTopSnackbar(binding.getRoot(), validation.getErrorMessage(), true);
            }
        });

        viewModel.getVerifyResult().observe(getViewLifecycleOwner(),
                resultObserver(
                        ignored -> navigateToNewPassword(),
                        this::showNetworkError));

        viewModel.getResendResult().observe(getViewLifecycleOwner(),
            resultObserver(
                ignored -> showResendSuccess(),
                this::showNetworkError));
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        applyActionLoadingState(
            binding.btnVerify,
            binding.progressVerifyInline,
            binding.tvVerifyButtonText,
            isLoading,
            R.string.btn_confirm
        );
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> popBackStackSafely());
        binding.btnVerify.setOnClickListener(v -> attemptVerifyOtp());
        binding.tvResend.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(viewModel.getResendEnabled().getValue())) {
                viewModel.resendOtp();
            }
        });
    }

    private void attemptVerifyOtp() {
        clearInputFocusAndHideKeyboard();
        String otp = otpInputController.collectOtp();
        if (otp.length() < 6) {
            showTopSnackbar(binding.getRoot(), getString(R.string.error_otp_length), true);
            return;
        }
        viewModel.verify(otp);
    }

    private void showNetworkError(String message) {
        showTopSnackbar(binding.getRoot(), message, true);
    }

    private void showResendSuccess() {
        showTopSnackbar(binding.getRoot(), getString(R.string.otp_resend_now), false);
    }

    private void clearOtpBoxes() {
        otpInputController.clear();
    }

    private void navigateToNewPassword() {
        Bundle args = new Bundle();
        args.putString("email", viewModel.getEmail());
        navigateSafely(R.id.action_forgotOtpFragment_to_forgotNewPasswordFragment, args);
    }
}
