package com.drc.aidbridge.ui.auth.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.DialogOtpFailedBinding;
import com.drc.aidbridge.databinding.DialogOtpSuccessBinding;
import com.drc.aidbridge.databinding.FragmentOtpBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.common.OtpInputController;
import com.drc.aidbridge.ui.auth.viewmodel.OtpViewModel;

import java.util.Arrays;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class OtpFragment extends BaseFragment<FragmentOtpBinding> {

    private OtpViewModel viewModel;
    private OtpInputController otpInputController;

    @Nullable
    @Override
    protected FragmentOtpBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentOtpBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(OtpViewModel.class);

        otpInputController = new OtpInputController(
            Arrays.asList(
                binding.etOtp1,
                binding.etOtp2,
                binding.etOtp3,
                binding.etOtp4,
                binding.etOtp5,
                binding.etOtp6
            ),
            this::updateBoxBackground
        );

        setupEmail();
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

        viewModel.getVerifyResult().observe(getViewLifecycleOwner(),
                resultObserver(binding.btnVerify,
                        ignored -> showSuccessDialog(),
                        message -> showFailedDialog()));

        viewModel.getResendResult().observe(getViewLifecycleOwner(),
            resultObserver(binding.tvResend,
                ignored -> showToast(getString(R.string.otp_resend_now)),
                this::showToast));
    }

    private void setupEmail() {
        String email = viewModel.getEmail();
        if (email != null && !email.isEmpty()) {
            binding.tvEmail.setText(email);
        }
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        binding.btnVerify.setOnClickListener(v -> attemptVerify());

        binding.tvResend.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(viewModel.getResendEnabled().getValue())) {
                viewModel.resendOtp();
            }
        });
    }

    @Override
    protected ProgressBar getLoadingView() {
        return binding.progressBar;
    }

    private void attemptVerify() {
        viewModel.verify(otpInputController.collectOtp());
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

    private Dialog buildDialog() {
        Dialog dialog = new Dialog(requireContext(), R.style.Theme_AidBridge_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    private void updateBoxBackground(EditText box, boolean filled) {
        box.setBackgroundResource(filled ? R.drawable.bg_otp_box_active : R.drawable.bg_otp_box);
    }

    private void navigateToLogin() {
        Navigation.findNavController(requireView())
                .navigate(R.id.action_otpFragment_to_loginFragment);
    }
}
