package com.drc.aidbridge.ui.main.fragment.admin;

import android.os.Bundle;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.FragmentAdminHubManagementBinding;
import com.drc.aidbridge.domain.enums.HubStatus;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.model.admin.HubSummary;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.admin.AdminHubAdapter;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminHubManagementViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Collections;
import java.util.List;

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
    }

    private void setupClickListeners() {
        binding.buttonAdminHubBack.setOnClickListener(v -> popBackStackSafely());
        binding.buttonAdminHubRetry.setOnClickListener(v -> refreshHubData());
        binding.fabAdminAddHub.setOnClickListener(v ->
                navigateSafely(R.id.action_adminHubManagementFragment_to_adminAddHubFragment));
    }

    @Override
    protected void observeViewModel() {
        viewModel.getHubsResult().observe(getViewLifecycleOwner(),
                resultObserver(this::renderHubs, this::renderHubError));

        viewModel.getToggleHubStatusResult().observe(getViewLifecycleOwner(), this::renderToggleStatusResult);

        viewModel.getHubSummary().observe(getViewLifecycleOwner(), this::renderSummary);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHubData();
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        if (binding == null) {
            return;
        }
        binding.progressAdminHubs.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            binding.recyclerAdminHubs.setVisibility(View.GONE);
            binding.layoutAdminHubEmpty.setVisibility(View.GONE);
            binding.layoutAdminHubError.setVisibility(View.GONE);
        }
    }

    @Override
    public void onViewHubDetails(@NonNull Hub hub) {
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
        HubStatus currentStatus = HubStatus.fromStringSafe(hub.getStatus());
        boolean deactivate = currentStatus == HubStatus.ACTIVE;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(deactivate
                        ? R.string.admin_hub_mgmt_confirm_deactivate_title
                        : R.string.admin_hub_mgmt_confirm_activate_title)
                .setMessage(deactivate
                        ? R.string.admin_hub_mgmt_confirm_deactivate_message
                        : R.string.admin_hub_mgmt_confirm_activate_message)
                .setNegativeButton(R.string.admin_hub_mgmt_confirm_cancel, null)
                .setPositiveButton(R.string.admin_hub_mgmt_confirm_positive,
                        (dialog, which) -> viewModel.toggleHubStatus(hub.getId()))
                .show();
    }

    private void refreshHubData() {
        viewModel.fetchHubs();
    }

    private void renderHubs(@Nullable List<Hub> hubs) {
        List<Hub> safeHubs = hubs != null ? hubs : Collections.emptyList();
        hubAdapter.submitList(safeHubs);

        boolean isEmpty = safeHubs.isEmpty();
        binding.recyclerAdminHubs.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.layoutAdminHubEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.layoutAdminHubError.setVisibility(View.GONE);
        if (isEmpty) {
            boolean searching = binding.editTextAdminHubSearch.getText() != null
                    && !binding.editTextAdminHubSearch.getText().toString().trim().isEmpty();
            binding.textAdminHubEmpty.setText(searching
                    ? R.string.admin_hub_mgmt_empty_search
                    : R.string.admin_hub_mgmt_empty);
        }
    }

    private void renderSummary(@Nullable HubSummary summary) {
        binding.textAdminHubTotal.setText(String.valueOf(summary != null ? summary.getTotalHubs() : 0));
        binding.textAdminHubActive.setText(String.valueOf(summary != null ? summary.getActiveHubs() : 0));
    }

    private void renderToggleStatusResult(@Nullable NetworkResultWrapper<Hub> result) {
        if (result == null || result.isLoading() || result.hasBeenHandled()) {
            return;
        }

        result.markAsHandled();
        if (result.isSuccess()) {
            handleToggleHubStatusSuccess(result.getData());
            return;
        }

        if (result.isError()) {
            String message = result.getMessage();
            showToast(message != null && !message.trim().isEmpty()
                    ? message
                    : getString(R.string.admin_hub_mgmt_error_generic));
        }
    }

    private void handleToggleHubStatusSuccess(@Nullable Hub updatedHub) {
        if (updatedHub == null) {
            return;
        }

        String hubName = resolveHubName(updatedHub);
        int statusRes = resolveStatusText(HubStatus.fromStringSafe(updatedHub.getStatus()));
        showToast(getString(R.string.admin_hub_mgmt_toast_status_changed, hubName, getString(statusRes)));
    }

    private void renderHubError(@NonNull String message) {
        hubAdapter.submitList(Collections.emptyList());
        binding.recyclerAdminHubs.setVisibility(View.GONE);
        binding.layoutAdminHubEmpty.setVisibility(View.GONE);
        binding.layoutAdminHubError.setVisibility(View.VISIBLE);
        Log.e("AdminHubMgmt", "Hub load error: " + message);
        binding.textAdminHubError.setText(getString(R.string.admin_hub_mgmt_error_generic));
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
