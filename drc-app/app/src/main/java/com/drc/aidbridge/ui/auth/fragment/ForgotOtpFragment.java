package com.drc.aidbridge.ui.auth.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.FragmentForgotOtpBinding;
import com.drc.aidbridge.domain.usecase.validation.AuthValidationResult;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.common.OtpInputController;
import com.drc.aidbridge.ui.auth.viewmodel.ForgotOtpViewModel;

import java.util.Arrays;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * ForgotOtpFragment â€” Forgot Password: Step 2 â€” Enter OTP.
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
            (box, filled) -> viewModel.clearOtpInlineError(),
            otp -> attemptVerifyOtp()
        );
        otpInputController.bind();

        setupClickListeners();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getCountdown().observe(getViewLifecycleOwner(),
            seconds -> renderResendState(viewModel.getResendUiState().getValue(), seconds));

        viewModel.getResendUiState().observe(getViewLifecycleOwner(),
            state -> renderResendState(state, viewModel.getCountdown().getValue()));

        viewModel.getOtpInlineErrorResId().observe(getViewLifecycleOwner(), this::renderInlineValidationError);

        viewModel.getValidationError().observe(getViewLifecycleOwner(), validation -> {
            if (validation == null || validation.isValid()) {
                return;
            }

            showTopSnackbar(binding.getRoot(), validation.getErrorMessage(), true);
        });

        viewModel.getVerifyResult().observe(getViewLifecycleOwner(),
                resultObserver(
                        this::handleVerifySuccess,
                        this::showNetworkError));

        viewModel.getResendResult().observe(getViewLifecycleOwner(), this::handleResendResult);
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
        binding.tvResend.setOnClickListener(v -> viewModel.resendOtp());
    }

    private void attemptVerifyOtp() {
        clearInputFocusAndHideKeyboard();
        String otp = otpInputController.collectOtp();
        viewModel.verify(otp);
    }

    private void showNetworkError(String message) {
        showTopSnackbar(binding.getRoot(), message, true);
    }

    private void handleVerifySuccess(String verifiedOtp) {
        navigateToNewPassword(verifiedOtp != null ? verifiedOtp : otpInputController.collectOtp());
    }

    private void handleResendResult(NetworkResultWrapper<Boolean> result) {
        if (result == null || result.isLoading() || result.hasBeenHandled()) {
            return;
        }

        result.markAsHandled();

        if (result.isSuccess()) {
            showTopSnackbar(binding.getRoot(), getString(R.string.otp_resend_success), false);
            otpInputController.clear();
            viewModel.clearOtpInlineError();
            return;
        }

        if (result.isError()) {
            String message = result.getMessage() != null
                ? result.getMessage()
                : getString(R.string.error_generic);
            showTopSnackbar(binding.getRoot(), message, true);
        }
    }

    private void renderInlineValidationError(Integer messageResId) {
        if (messageResId != null) {
            showTopSnackbar(binding.getRoot(), getString(messageResId), true);
        }
    }

    private void renderResendState(ForgotOtpViewModel.ResendUiState state, Integer seconds) {
        ForgotOtpViewModel.ResendUiState safeState = state != null
            ? state
            : ForgotOtpViewModel.ResendUiState.TIMER_RUNNING;

        if (safeState == ForgotOtpViewModel.ResendUiState.LOADING) {
            binding.tvResend.setText(R.string.otp_resend_loading);
            binding.tvResend.setEnabled(false);
            binding.tvResend.setClickable(false);
            binding.tvResend.setTextColor(requireContext().getColor(R.color.text_secondary));
            return;
        }

        if (safeState == ForgotOtpViewModel.ResendUiState.READY) {
            binding.tvResend.setText(R.string.otp_resend_ready);
            binding.tvResend.setEnabled(true);
            binding.tvResend.setClickable(true);
            binding.tvResend.setTextColor(requireContext().getColor(R.color.text_link));
            return;
        }

        int safeSeconds = (seconds != null && seconds > 0) ? seconds : 0;
        binding.tvResend.setText(getString(R.string.otp_resend_countdown_template, safeSeconds));
        binding.tvResend.setEnabled(false);
        binding.tvResend.setClickable(false);
        binding.tvResend.setTextColor(requireContext().getColor(R.color.text_secondary));
    }

    private void navigateToNewPassword(String verifiedOtp) {
        Bundle args = new Bundle();
        args.putString("email", viewModel.getEmail());
        args.putString("otp", verifiedOtp != null ? verifiedOtp : "");
        navigateSafely(R.id.action_forgotOtpFragment_to_forgotNewPasswordFragment, args);
    }
}

