package com.drc.aidbridge.ui.main.fragment.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminProfileBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.MainActivity;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminProfileViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminProfileFragment extends BaseFragment<FragmentAdminProfileBinding> {

    private AdminProfileViewModel viewModel;

    @Nullable
    @Override
    protected FragmentAdminProfileBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentAdminProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AdminProfileViewModel.class);
        binding.textAdminProfileTitle.setText(getString(R.string.admin_profile_title));

        viewModel.loadCurrentUserInfo();
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.layoutAdminChangeInfo
                .setOnClickListener(v -> showToast(getString(R.string.admin_profile_toast_change_info)));
        binding.layoutAdminManageRoles
                .setOnClickListener(v -> showToast(getString(R.string.admin_profile_toast_manage_roles)));
        binding.layoutAdminSystemSettings
                .setOnClickListener(v -> showToast(getString(R.string.admin_profile_toast_system_settings)));
        binding.buttonAdminLogout
                .setOnClickListener(v -> requestLogout());
    }

    @Override
    protected void observeViewModel() {
        viewModel.getEmail().observe(getViewLifecycleOwner(), email -> {
            if (email != null) {
                binding.textAdminProfileEmail.setText(email);
            }
        });

        viewModel.getPhone().observe(getViewLifecycleOwner(), phone -> {
            if (phone != null) {
                binding.textAdminProfilePhone.setText(phone);
            }
        });
    }

    private void requestLogout() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).requestLogout();
        }
    }
}