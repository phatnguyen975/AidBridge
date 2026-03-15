package com.drc.aidbridge.ui.auth.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ProgressBar;

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
            null
        );
        otpInputController.bind();

        setupClickListeners();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getValidationError().observe(getViewLifecycleOwner(), validation -> {
            if (validation == null || validation.isValid()) {
                return;
            }

            if (validation.getErrorField() == ValidationResult.Field.OTP) {
                clearOtpBoxes();
            } else {
                showToast(validation.getErrorMessage());
            }
        });

        viewModel.getVerifyResult().observe(getViewLifecycleOwner(),
                resultObserver(binding.btnVerify,
                        ignored -> navigateToNewPassword(),
                        this::showNetworkError));

        viewModel.getResendResult().observe(getViewLifecycleOwner(),
            resultObserver(binding.tvResend,
                ignored -> showResendSuccess(),
                this::showNetworkError));
    }

    @Override
    protected ProgressBar getLoadingView() {
        return binding.progressBar;
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> popBackStackSafely());
        binding.btnVerify.setOnClickListener(v -> attemptVerifyOtp());
        binding.tvResend.setOnClickListener(v -> viewModel.resendOtp());
    }

    private void attemptVerifyOtp() {
        viewModel.verify(otpInputController.collectOtp());
    }

    private void showNetworkError(String message) {
        showToast(message);
    }

    private void showResendSuccess() {
        showToast(getString(R.string.otp_resend_now));
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
