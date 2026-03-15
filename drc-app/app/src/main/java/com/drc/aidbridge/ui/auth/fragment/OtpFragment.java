package com.drc.aidbridge.ui.auth.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.DialogOtpFailedBinding;
import com.drc.aidbridge.databinding.DialogOtpSuccessBinding;
import com.drc.aidbridge.databinding.FragmentOtpBinding;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.common.OtpInputController;
import com.drc.aidbridge.ui.auth.viewmodel.OtpViewModel;

import java.util.Arrays;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * OtpFragment — handles OTP input and verification for registration.
 */
@AndroidEntryPoint
public class OtpFragment extends BaseFragment<FragmentOtpBinding> {

    private OtpViewModel viewModel;
    private OtpInputController otpInputController;

    @Override
    protected FragmentOtpBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentOtpBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(OtpViewModel.class);
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
            otp -> attemptVerify()
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
                otpInputController.clear();
            } else {
                showToast(validation.getErrorMessage());
            }
        });

        viewModel.getVerifyResult().observe(getViewLifecycleOwner(),
                resultObserver(binding.btnVerify,
                        ignored -> showSuccessDialog(),
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

        binding.btnVerify.setOnClickListener(v -> attemptVerify());

        binding.tvResend.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(viewModel.getResendEnabled().getValue())) {
                viewModel.resendOtp();
            }
        });
    }

    private void attemptVerify() {
        String otp = otpInputController.collectOtp();
        if (otp.length() < 6) {
            showToast(getString(R.string.error_otp_length));
            return;
        }
        viewModel.verify(otp);
    }

    private void showSuccessDialog() {
        Dialog dialog = buildDialog();
        DialogOtpSuccessBinding dialogBinding = DialogOtpSuccessBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        dialogBinding.btnContinue.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToLogin();
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    private void showFailedDialog() {
        Dialog dialog = buildDialog();
        DialogOtpFailedBinding dialogBinding = DialogOtpFailedBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        dialogBinding.btnRetry.setOnClickListener(v -> {
            dialog.dismiss();
            otpInputController.clear();
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    private void showNetworkError(String message) {
        if (message != null && !message.trim().isEmpty()) {
            showToast(message);
        } else {
            showFailedDialog();
        }
    }

    private void showResendSuccess() {
        showToast(getString(R.string.otp_resend_now));
    }

    private Dialog buildDialog() {
        Dialog dialog = new Dialog(requireContext(), R.style.Theme_AidBridge_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    private void navigateToLogin() {
        navigateSafely(R.id.action_otpFragment_to_loginFragment);
    }
}
