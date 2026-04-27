package com.drc.aidbridge.ui.main.fragment.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminHubManagementBinding;
import com.drc.aidbridge.domain.enums.HubStatus;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.admin.AdminHubAdapter;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminHubManagementViewModel;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminHubManagementFragment extends BaseFragment<FragmentAdminHubManagementBinding>
        implements AdminHubAdapter.HubActionListener {

    private AdminHubManagementViewModel viewModel;
    private AdminHubAdapter hubAdapter;

    @Nullable
    @Override
    protected FragmentAdminHubManagementBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAdminHubManagementBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AdminHubManagementViewModel.class);
        hubAdapter = new AdminHubAdapter(this);

        binding.recyclerAdminHubs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerAdminHubs.setAdapter(hubAdapter);

        binding.editTextAdminHubSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.updateSearchQuery(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        setupClickListeners();
        viewModel.fetchHubs();
    }

    private void setupClickListeners() {
        binding.buttonAdminHubBack.setOnClickListener(v -> popBackStackSafely());
        binding.fabAdminAddHub.setOnClickListener(v -> showToast(getString(R.string.admin_hub_mgmt_toast_add_new)));
    }

    @Override
    protected void observeViewModel() {
        viewModel.getHubsResult().observe(getViewLifecycleOwner(),
                resultObserver(this::renderHubs, this::showHubLoadError));

        viewModel.getToggleHubStatusResult().observe(getViewLifecycleOwner(),
                resultObserver(this::handleToggleHubStatusSuccess, this::showHubLoadError));

        viewModel.getTotalHubsCount().observe(getViewLifecycleOwner(),
                count -> binding.textAdminHubTotal.setText(String.valueOf(count != null ? count : 0)));

        viewModel.getActiveHubsCount().observe(getViewLifecycleOwner(),
                count -> binding.textAdminHubActive.setText(String.valueOf(count != null ? count : 0)));
    }

    @Override
    public void onViewHubDetails(@NonNull Hub hub) {
        String hubName = resolveHubName(hub);
        showToast(getString(R.string.admin_hub_mgmt_toast_open_details, hubName));

        if (hub.getId() == null) {
            showToast(getString(R.string.admin_hub_detail_error_invalid_hub_id));
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(AdminHubDetailFragment.ARG_HUB_ID, hub.getId().toString());
        navigateSafely(R.id.action_adminHubManagementFragment_to_adminHubDetailFragment, bundle);
    }

    @Override
    public void onToggleHubStatus(@NonNull Hub hub) {
        viewModel.toggleHubStatus(hub.getId());
    }

    private void renderHubs(@Nullable java.util.List<Hub> hubs) {
        if (hubs == null) {
            hubAdapter.submitList(new ArrayList<>());
            return;
        }
        hubAdapter.submitList(hubs);
    }

    private void handleToggleHubStatusSuccess(@Nullable Hub updatedHub) {
        if (updatedHub == null) {
            return;
        }

        String hubName = resolveHubName(updatedHub);
        int statusRes = resolveStatusText(HubStatus.fromStringSafe(updatedHub.getStatus()));
        showToast(getString(R.string.admin_hub_mgmt_toast_status_changed, hubName, getString(statusRes)));
    }

    private void showHubLoadError(@NonNull String message) {
        if (message.trim().isEmpty()) {
            showToast(getString(R.string.admin_hub_mgmt_error_generic));
            return;
        }
        showToast(message);
    }

    @NonNull
    private String resolveHubName(@NonNull Hub hub) {
        String hubName = hub.getName();
        if (hubName == null || hubName.trim().isEmpty()) {
            return getString(R.string.admin_hub_mgmt_name_fallback);
        }
        return hubName;
    }

    private int resolveStatusText(@Nullable HubStatus status) {
        if (status == HubStatus.ACTIVE) {
            return R.string.admin_hub_mgmt_status_active;
        }
        if (status == HubStatus.EMERGENCY) {
            return R.string.admin_hub_mgmt_status_emergency;
        }
        return R.string.admin_hub_mgmt_status_inactive;
    }
}