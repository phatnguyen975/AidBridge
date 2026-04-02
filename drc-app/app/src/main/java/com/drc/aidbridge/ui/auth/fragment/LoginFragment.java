package com.drc.aidbridge.ui.auth.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentLoginBinding;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.auth.viewmodel.LoginViewModel;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.MainActivity;
import com.google.firebase.messaging.FirebaseMessaging;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * LoginFragment — handles user input for email/password authentication.
 */
@AndroidEntryPoint
public class LoginFragment extends BaseFragment<FragmentLoginBinding> {

    private LoginViewModel viewModel;
    private String pendingFcmToken;

    @Override
    protected FragmentLoginBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentLoginBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        requestFcmToken();
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
            } else if (validation.getErrorField() == ValidationResult.Field.PASSWORD) {
                binding.tilPassword.setError(validation.getErrorMessage());
                binding.tilPassword.requestFocus();
            } else {
                showTopSnackbar(binding.getRoot(), validation.getErrorMessage(), true);
            }
        });

        viewModel.getLoginResult().observe(getViewLifecycleOwner(),
                resultObserver(this::handleLoginSuccess,
                        this::showNetworkError));
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        applyActionLoadingState(
            binding.btnLogin,
            binding.progressLoginInline,
            binding.tvLoginButtonText,
            isLoading,
            R.string.btn_login_action
        );
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> popBackStackSafely(R.id.guestShellFragment, false));
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.tvForgotPassword.setOnClickListener(v ->
            navigateSafely(R.id.action_loginFragment_to_forgotEmailFragment));
        binding.tvRegisterLink.setOnClickListener(v ->
            navigateSafely(R.id.action_loginFragment_to_registerFragment));
    }

    private void attemptLogin() {
        clearInputFocusAndHideKeyboard();
        String email = getRawText(binding.etEmail);
        String password = getRawText(binding.etPassword);
        viewModel.login(email, password, resolveDeviceId(), pendingFcmToken);
    }

    private void requestFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!isAdded()) {
                return;
            }

            if (!task.isSuccessful()) {
                pendingFcmToken = null;
                return;
            }

            String token = task.getResult();
            pendingFcmToken = token != null && !token.trim().isEmpty() ? token.trim() : null;
        });
    }

    private String resolveDeviceId() {
        String deviceId = Settings.Secure.getString(
            requireContext().getContentResolver(),
            Settings.Secure.ANDROID_ID
        );
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return getString(R.string.app_name);
        }
        return deviceId.trim();
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
        binding.tilPassword.setError(null);
    }

    private void navigateToMain() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void navigateToOtp(@Nullable String email) {
        Bundle args = new Bundle();
        args.putString("email", email != null ? email : "");
        navigateSafely(R.id.action_loginFragment_to_otpFragment, args);
    }

    private void handleLoginSuccess(@Nullable User user) {
        if (user == null) {
            showTopSnackbar(binding.getRoot(), getString(R.string.error_generic), true);
            return;
        }

        if (user.isVerified()) {
            navigateToMain();
            return;
        }

        navigateToOtp(user.getEmail());
    }
}
