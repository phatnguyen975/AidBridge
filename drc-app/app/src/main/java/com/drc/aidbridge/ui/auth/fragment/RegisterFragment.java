package com.drc.aidbridge.ui.auth.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentRegisterBinding;
import com.drc.aidbridge.domain.enums.UserRole;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import com.drc.aidbridge.ui.auth.viewmodel.RegisterViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RegisterFragment extends BaseFragment<FragmentRegisterBinding> {

    private RegisterViewModel viewModel;

    @Nullable
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

        viewModel.getRegisterResult().observe(getViewLifecycleOwner(),
                resultObserver(binding.btnRegister,
                        ignored -> navigateToOtp(getEmail()),
                        this::showInlineError));
    }

    private void setupRoleCards() {
        binding.cardVictim.setOnClickListener(v -> viewModel.selectRole(UserRole.VICTIM));
        binding.cardVolunteer.setOnClickListener(v -> viewModel.selectRole(UserRole.VOLUNTEER));
        binding.cardSponsor.setOnClickListener(v -> viewModel.selectRole(UserRole.SPONSOR));
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> navigateToGuest());

        binding.btnRegister.setOnClickListener(v -> attemptRegister());

        binding.tvLoginLink.setOnClickListener(v -> navigateToLogin());
    }

    private void navigateToGuest() {
        NavController navController = Navigation.findNavController(requireView());
        boolean popped = navController.popBackStack(R.id.guestFragment, false);
        if (!popped) {
            navController.navigate(R.id.guestFragment);
        }
    }

    private void navigateToLogin() {
        NavController navController = Navigation.findNavController(requireView());
        boolean popped = navController.popBackStack(R.id.loginFragment, false);
        if (!popped) {
            navController.navigate(R.id.action_registerFragment_to_loginFragment);
        }
    }

    private void attemptRegister() {
        clearErrors();

        String name = getTextOrEmpty(binding.tilName.getEditText());
        String email = getEmail();
        String phone = getTextOrEmpty(binding.tilPhone.getEditText());
        String password = getTextOrEmpty(binding.tilPassword.getEditText());

        if (!binding.cbTerms.isChecked()) {
            binding.cbTerms.setError("Vui lòng đồng ý điều khoản");
            return;
        }

        viewModel.register(name, email, phone, password);
    }

    private String getEmail() {
        return getTextOrEmpty(binding.tilEmail.getEditText());
    }

    private String getTextOrEmpty(@Nullable EditText et) {
        if (et == null) {
            return "";
        }
        return et.getText() != null ? et.getText().toString().trim() : "";
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

    private void showInlineError(String message) {
        if (message == null) {
            return;
        }

        String msg = message.toLowerCase();
        if (msg.contains("họ") || msg.contains("tên")) {
            binding.tilName.setError(message);
            binding.tilName.requestFocus();
            return;
        }
        if (msg.contains("email")) {
            binding.tilEmail.setError(message);
            binding.tilEmail.requestFocus();
            return;
        }
        if (msg.contains("điện thoại") || msg.contains("phone")) {
            binding.tilPhone.setError(message);
            binding.tilPhone.requestFocus();
            return;
        }
        if (msg.contains("mật khẩu") || msg.contains("khẩu")) {
            binding.tilPassword.setError(message);
            binding.tilPassword.requestFocus();
            return;
        }

        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(requireContext().getColor(R.color.sos_red))
                .setTextColor(requireContext().getColor(R.color.white))
                .show();
    }

    private void clearErrors() {
        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPhone.setError(null);
        binding.tilPassword.setError(null);
    }

    @Override
    protected ProgressBar getLoadingView() {
        return binding.progressBar;
    }

    private void navigateToOtp(String email) {
        Bundle args = new Bundle();
        args.putString("email", email);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_registerFragment_to_otpFragment, args);
    }
}
