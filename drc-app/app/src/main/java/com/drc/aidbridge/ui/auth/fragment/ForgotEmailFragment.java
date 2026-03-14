package com.drc.aidbridge.ui.auth.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentForgotEmailBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.auth.viewmodel.ForgotEmailViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * ForgotEmailFragment — Forgot Password: Step 1 — Enter Email.
 *
 * The user enters their registered email address.
 * On submit: validates format, then delegates to ForgotEmailViewModel which
 * contacts the server (currently mocked).
 * On success: navigates to ForgotOtpFragment.
 *
 * API: POST /auth/forgot-password  { "email": "..." }
 */
@AndroidEntryPoint
public class ForgotEmailFragment extends BaseFragment<FragmentForgotEmailBinding> {
    private ForgotEmailViewModel viewModel;

    @Override
    protected FragmentForgotEmailBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentForgotEmailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(ForgotEmailViewModel.class);
        setupClickListeners();
    }

    // ------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        binding.btnSendOtp.setOnClickListener(v -> attemptSendOtp());
    }

    @Override
    protected void observeViewModel() {
        viewModel.getSendOtpResult().observe(getViewLifecycleOwner(),
                resultObserver(binding.btnSendOtp,
                        ignored -> navigateToOtp(getEmail())));
    }

    // ------------------------------------------------------------------
    // Logic
    // ------------------------------------------------------------------

    /**
     * Validates the email field locally, then hands off to the ViewModel.
     * Local validation is intentionally lightweight — format only.
     * Business-level errors (e.g. "email not found") are returned by the server
     * and surfaced via the LiveData error state.
     */
    private void attemptSendOtp() {
        binding.tilEmail.setError(null);

        String email = getEmail();
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_field_empty));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_email_invalid));
            return;
        }

        viewModel.sendOtp(email);
    }

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    private void navigateToOtp(String email) {
        Bundle args = new Bundle();
        args.putString("email", email);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_forgotEmailFragment_to_forgotOtpFragment, args);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String getEmail() {
        if (binding.tilEmail.getEditText() == null) return "";
        CharSequence text = binding.tilEmail.getEditText().getText();
        return text != null ? text.toString().trim() : "";
    }
}
