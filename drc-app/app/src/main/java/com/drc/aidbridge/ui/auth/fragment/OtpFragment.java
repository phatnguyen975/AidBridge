package com.drc.aidbridge.ui.auth.fragment;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.DialogOtpSuccessBinding;
import com.drc.aidbridge.databinding.FragmentOtpBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.common.OtpInputController;
import com.drc.aidbridge.ui.main.MainActivity;
import com.drc.aidbridge.ui.auth.viewmodel.OtpViewModel;

import java.util.Arrays;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * OtpFragment — handles OTP input and verification for registration.
 */
@AndroidEntryPoint
public class OtpFragment extends BaseFragment<FragmentOtpBinding> {

    private static final float SUCCESS_DIALOG_WIDTH_RATIO = 0.9f;

    private OtpViewModel viewModel;
    private OtpInputController otpInputController;

    @Override
    protected FragmentOtpBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentOtpBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@androidx.annotation.NonNull View view,
                              @androidx.annotation.Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().getOnBackPressedDispatcher().addCallback(
            getViewLifecycleOwner(),
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    showTopSnackbar(binding.getRoot(), getString(R.string.otp_back_locked_message), true);
                    navigateSafely(R.id.action_otpFragment_to_loginFragment);
                }
            }
        );
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
            (box, filled) -> viewModel.clearOtpInlineError(),
            otp -> attemptVerify()
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
            ignored -> showSuccessDialog(),
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
        binding.btnVerify.setOnClickListener(v -> attemptVerify());
        binding.tvResend.setOnClickListener(v -> viewModel.resendOtp());
    }

    private void attemptVerify() {
        clearInputFocusAndHideKeyboard();
        String otp = otpInputController.collectOtp();
        viewModel.verify(otp);
    }

    private void showNetworkError(String message) {
        showTopSnackbar(binding.getRoot(), message, true);
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

    private void renderResendState(OtpViewModel.ResendUiState state, Integer seconds) {
        OtpViewModel.ResendUiState safeState = state != null
            ? state
            : OtpViewModel.ResendUiState.TIMER_RUNNING;

        if (safeState == OtpViewModel.ResendUiState.LOADING) {
            binding.tvResend.setText(R.string.otp_resend_loading);
            binding.tvResend.setEnabled(false);
            binding.tvResend.setClickable(false);
            binding.tvResend.setTextColor(requireContext().getColor(R.color.text_secondary));
            return;
        }

        if (safeState == OtpViewModel.ResendUiState.READY) {
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

    private void navigateToMain() {
        if (!isAdded()) {
            return;
        }

        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void showSuccessDialog() {
        if (!isAdded()) {
            return;
        }

        DialogOtpSuccessBinding dialogBinding = DialogOtpSuccessBinding.inflate(getLayoutInflater());
        AlertDialog successDialog = new AlertDialog.Builder(requireContext(), R.style.Theme_AidBridge_Dialog)
            .setView(dialogBinding.getRoot())
            .setCancelable(false)
            .create();

        dialogBinding.btnContinue.setOnClickListener(v -> {
            successDialog.dismiss();
            navigateToMain();
        });

        successDialog.show();

        if (successDialog.getWindow() != null) {
            int dialogWidth = (int) (getResources().getDisplayMetrics().widthPixels * SUCCESS_DIALOG_WIDTH_RATIO);
            successDialog.getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
