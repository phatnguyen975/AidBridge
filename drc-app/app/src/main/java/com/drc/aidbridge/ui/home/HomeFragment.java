package com.drc.aidbridge.ui.home;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentHomeBinding;
import com.drc.aidbridge.ui.auth.AuthActivity;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.utils.TokenManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends BaseFragment<FragmentHomeBinding> {

    @Inject
    TokenManager tokenManager;

    @Nullable
    @Override
    protected FragmentHomeBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentHomeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        populateUserInfo();
        binding.btnLogout.setOnClickListener(v -> logout());
    }

    @Override
    protected void observeViewModel() {
    }

    private void populateUserInfo() {
        String name = tokenManager.getUserName();
        String email = tokenManager.getUserEmail();
        String role = tokenManager.getUserRole();

        binding.tvUserName.setText(name != null && !name.isEmpty() ? name : "Người dùng");
        binding.tvUserEmail.setText(email != null ? email : "");
        binding.tvUserRole.setText(formatRole(role));
    }

    private String formatRole(String role) {
        if (role == null) {
            return "GUEST";
        }
        switch (role) {
            case "VICTIM":
                return "Nạn nhân";
            case "VOLUNTEER":
                return "Tình nguyện viên";
            case "SPONSOR":
                return "Mạnh thường quân";
            case "STAFF":
                return "Nhân viên trạm";
            case "ADMIN":
                return "Quản trị viên";
            default:
                return role;
        }
    }

    private void logout() {
        tokenManager.clearAll();

        Intent intent = new Intent(requireActivity(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
