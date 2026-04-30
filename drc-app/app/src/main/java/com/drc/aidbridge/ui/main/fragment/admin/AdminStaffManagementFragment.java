package com.drc.aidbridge.ui.main.fragment.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminStaffManagementBinding;
import com.drc.aidbridge.domain.model.admin.Staff;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.admin.AdminStaffAdapter;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminStaffManagementViewModel;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminStaffManagementFragment extends BaseFragment<FragmentAdminStaffManagementBinding> {

    private AdminStaffManagementViewModel viewModel;
    private AdminStaffAdapter adapter;

    @Nullable
    @Override
    protected FragmentAdminStaffManagementBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAdminStaffManagementBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AdminStaffManagementViewModel.class);
        adapter = new AdminStaffAdapter();
        binding.recyclerAdminStaff.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerAdminStaff.setAdapter(adapter);

        binding.buttonAdminStaffBack.setOnClickListener(v -> popBackStackSafely());
        binding.buttonAdminStaffRetry.setOnClickListener(v -> viewModel.fetchStaff());
        binding.fabAdminAddStaff.setOnClickListener(v ->
                navigateSafely(R.id.action_adminStaffManagementFragment_to_adminAddStaffFragment));
    }

    @Override
    protected void observeViewModel() {
        viewModel.getStaffResult().observe(
                getViewLifecycleOwner(),
                resultObserver(this::renderStaff, this::renderError)
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.fetchStaff();
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        if (binding == null) {
            return;
        }
        binding.progressAdminStaff.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            binding.recyclerAdminStaff.setVisibility(View.GONE);
            binding.layoutAdminStaffEmpty.setVisibility(View.GONE);
            binding.layoutAdminStaffError.setVisibility(View.GONE);
        }
    }

    private void renderStaff(@Nullable List<Staff> staffList) {
        List<Staff> safeList = staffList != null ? staffList : java.util.Collections.emptyList();
        adapter.submitList(safeList);

        boolean isEmpty = safeList.isEmpty();
        binding.recyclerAdminStaff.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.layoutAdminStaffEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.layoutAdminStaffError.setVisibility(View.GONE);
    }

    private void renderError(@NonNull String message) {
        adapter.submitList(java.util.Collections.emptyList());
        binding.recyclerAdminStaff.setVisibility(View.GONE);
        binding.layoutAdminStaffEmpty.setVisibility(View.GONE);
        binding.layoutAdminStaffError.setVisibility(View.VISIBLE);
        binding.textAdminStaffError.setText(
                message.trim().isEmpty()
                        ? getString(R.string.admin_staff_error_generic)
                        : message
        );
    }
}
