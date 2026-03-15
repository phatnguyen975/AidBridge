package com.drc.aidbridge.ui.auth.fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentLoginBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.auth.viewmodel.LoginViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * LoginFragment — handles user input for email/password authentication.
 */
@AndroidEntryPoint
public class LoginFragment extends BaseFragment<FragmentLoginBinding> {

    private LoginViewModel viewModel;

    @Override
    protected FragmentLoginBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentLoginBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Back arrow → always return to SOS Guest screen
        binding.btnBack.setOnClickListener(v -> navigateToGuest());

        // Login button — validates inputs before calling ViewModel
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        // "Quên mật khẩu?" → ForgotEmailFragment
        binding.tvForgotPassword.setOnClickListener(v ->
            navigateSafely(R.id.action_loginFragment_to_forgotEmailFragment));

        // "Đăng ký ngay" → RegisterFragment
        binding.tvRegisterLink.setOnClickListener(v ->
            navigateSafely(R.id.action_loginFragment_to_registerFragment));
    }

    @Override
    protected void observeViewModel() {
        viewModel.getLoginResult().observe(getViewLifecycleOwner(),
                resultObserver(binding.btnLogin,
                        ignored -> navigateToMain(),
                        this::showLoginError));
    }

    @Override
    protected ProgressBar getLoadingView() {
        return binding.progressBar;
    }

    private void attemptLogin() {
        String email = getTextOrEmpty(binding.etEmail);
        String password = getTextOrEmpty(binding.etPassword);

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        boolean valid = true;
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_field_empty));
            binding.tilEmail.requestFocus();
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_field_empty));
            if (valid) binding.tilPassword.requestFocus();
            valid = false;
        }

        if (valid) {
            viewModel.login(email, password);
        }
    }

    private String getTextOrEmpty(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void showLoginError(String message) {
        if (message == null) {
            return;
        }

        String lower = message.toLowerCase();
        if (lower.contains("email")) {
            binding.tilEmail.setError(message);
            binding.tilEmail.requestFocus();
            return;
        }
        if (lower.contains("mật khẩu") || lower.contains("password")) {
            binding.tilPassword.setError(message);
            binding.tilPassword.requestFocus();
            return;
        }

        showToast(message);
    }

    private void navigateToMain() {
        navigateSafely(R.id.action_loginFragment_to_mainActivity);
    }

    private void navigateToGuest() {
        NavController navController = getViewNavController();
        if (navController == null) {
            return;
        }

        boolean popped = popBackStackSafely(navController, R.id.guestShellFragment, false);
        if (!popped) {
            navigateToDestinationSafely(navController, R.id.guestShellFragment);
        }
    }
}
