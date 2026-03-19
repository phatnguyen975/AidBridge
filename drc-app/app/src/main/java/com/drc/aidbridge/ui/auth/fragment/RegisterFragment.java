package com.drc.aidbridge.ui.auth.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentRegisterBinding;
import com.drc.aidbridge.domain.enums.UserRole;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.auth.viewmodel.RegisterViewModel;

import com.google.android.material.card.MaterialCardView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RegisterFragment extends BaseFragment<FragmentRegisterBinding> {

    private RegisterViewModel viewModel;

    @Override
    protected FragmentRegisterBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentRegisterBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);
        setupRoleCards();
        setupClickListeners();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getSelectedRole().observe(getViewLifecycleOwner(), this::updateRoleCardSelection);

        viewModel.getValidationError().observe(getViewLifecycleOwner(), validation -> {
            clearErrors();

            if (validation == null || validation.isValid()) {
                return;
            }

            if (validation.getErrorField() == ValidationResult.Field.NAME) {
                binding.tilName.setError(validation.getErrorMessage());
                binding.tilName.requestFocus();
            } else if (validation.getErrorField() == ValidationResult.Field.EMAIL) {
                binding.tilEmail.setError(validation.getErrorMessage());
                binding.tilEmail.requestFocus();
            } else if (validation.getErrorField() == ValidationResult.Field.PHONE) {
                binding.tilPhone.setError(validation.getErrorMessage());
                binding.tilPhone.requestFocus();
            } else if (validation.getErrorField() == ValidationResult.Field.PASSWORD) {
                binding.tilPassword.setError(validation.getErrorMessage());
                binding.tilPassword.requestFocus();
            } else {
                showToast(validation.getErrorMessage());
            }
        });

        viewModel.getRegisterResult().observe(getViewLifecycleOwner(),
                resultObserver(binding.btnRegister,
                        ignored -> navigateToOtp(getRawText(binding.tilEmail.getEditText())),
                        this::showNetworkError));
    }

    @Override
    protected ProgressBar getLoadingView() {
        return binding.progressBar;
    }

    private void setupRoleCards() {
        binding.cardVictim.setOnClickListener(v -> viewModel.selectRole(UserRole.VICTIM));
        binding.cardVolunteer.setOnClickListener(v -> viewModel.selectRole(UserRole.VOLUNTEER));
        binding.cardSponsor.setOnClickListener(v -> viewModel.selectRole(UserRole.SPONSOR));
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> popBackStackSafely(R.id.guestShellFragment, false));
        binding.btnRegister.setOnClickListener(v -> attemptRegister());

        binding.tvLoginLink.setOnClickListener(v -> 
            navigateSafely(R.id.action_registerFragment_to_loginFragment)
        );
    }

    private void attemptRegister() {
        if (!binding.cbTerms.isChecked()) {
            showToast(getString(R.string.error_terms_not_agreed));
            return;
        }

        String name = getRawText(binding.tilName.getEditText());
        String email = getRawText(binding.tilEmail.getEditText());
        String phone = getRawText(binding.tilPhone.getEditText());
        String password = getRawText(binding.tilPassword.getEditText());
        viewModel.register(name, email, phone, password);
    }

    private String getRawText(@Nullable EditText et) {
        if (et == null) {
            return "";
        }
        return et.getText() != null ? et.getText().toString() : "";
    }

    private void showNetworkError(String message) {
        showToast(message);
    }

    private void clearErrors() {
        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPhone.setError(null);
        binding.tilPassword.setError(null);
    }

    private void navigateToOtp(String email) {
        Bundle args = new Bundle();
        args.putString("email", email);
        navigateSafely(R.id.action_registerFragment_to_otpFragment, args);
    }

    private void updateRoleCardSelection(@Nullable UserRole role) {
        if (role == null) {
            resetCardStroke(binding.cardVictim);
            resetCardStroke(binding.cardVolunteer);
            resetCardStroke(binding.cardSponsor);
            return;
        }

        switch (role) {
            case VICTIM:
                selectCardStroke(binding.cardVictim);
                resetCardStroke(binding.cardVolunteer);
                resetCardStroke(binding.cardSponsor);
                break;
            case VOLUNTEER:
                resetCardStroke(binding.cardVictim);
                selectCardStroke(binding.cardVolunteer);
                resetCardStroke(binding.cardSponsor);
                break;
            case SPONSOR:
                resetCardStroke(binding.cardVictim);
                resetCardStroke(binding.cardVolunteer);
                selectCardStroke(binding.cardSponsor);
                break;
            default:
                resetCardStroke(binding.cardVictim);
                resetCardStroke(binding.cardVolunteer);
                resetCardStroke(binding.cardSponsor);
        }
    }

    private void selectCardStroke(MaterialCardView card) {
        card.setStrokeColor(requireContext().getColor(R.color.color_primary));
        card.setStrokeWidth(dpToPx(2));
    }

    private void resetCardStroke(MaterialCardView card) {
        card.setStrokeColor(requireContext().getColor(R.color.border_default));
        card.setStrokeWidth(dpToPx(1));
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
