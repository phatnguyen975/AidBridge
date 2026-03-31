package com.drc.aidbridge.ui.main.fragment.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminDashboardBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminDashboardViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminDashboardFragment extends BaseFragment<FragmentAdminDashboardBinding> {

    private AdminDashboardViewModel viewModel;

    @Nullable
    @Override
    protected FragmentAdminDashboardBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentAdminDashboardBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AdminDashboardViewModel.class);
        binding.textAdminDashboardTitle.setText(getString(R.string.admin_dashboard_title));

        binding.buttonAdminOpenMap.setOnClickListener(this::onOpenMapClicked);
        binding.buttonAdminManageHub.setOnClickListener(this::onManageHubClicked);
    }

    @Override
    protected void observeViewModel() {
    }

    private void onOpenMapClicked(View view) {
        showToast(getString(R.string.admin_dashboard_open_map_todo));
    }

    private void onManageHubClicked(View view) {
        showToast(getString(R.string.admin_dashboard_manage_hubs_todo));
    }
}
