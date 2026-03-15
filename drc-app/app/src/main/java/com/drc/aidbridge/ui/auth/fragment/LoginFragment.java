package com.drc.aidbridge.ui.auth.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentLoginBinding;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.auth.viewmodel.LoginViewModel;
import com.drc.aidbridge.ui.base.BaseFragment;

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
                showToast(validation.getErrorMessage());
            }
        });

        viewModel.getLoginResult().observe(getViewLifecycleOwner(),
                resultObserver(binding.btnLogin,
                        ignored -> navigateToMain(),
                        this::showNetworkError));
    }

    @Override
    protected ProgressBar getLoadingView() {
        return binding.progressBar;
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
        String email = getRawText(binding.etEmail);
        String password = getRawText(binding.etPassword);
        viewModel.login(email, password);
    }

    private String getRawText(EditText et) {
        if (et == null) {
            return "";
        }
        return et.getText() != null ? et.getText().toString() : "";
    }

    private void showNetworkError(String message) {
        showToast(message);
    }

    private void clearErrors() {
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
    }

    private void navigateToMain() {
        navigateSafely(R.id.action_loginFragment_to_mainActivity);
    }
}
