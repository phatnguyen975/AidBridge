package com.drc.aidbridge.ui.main.fragment.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminHubDetailBinding;
import com.drc.aidbridge.domain.enums.HubStatus;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.admin.AdminHubDetailInventoryAdapter;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminHubDetailViewModel;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminHubDetailFragment extends BaseFragment<FragmentAdminHubDetailBinding> {

    public static final String ARG_HUB_ID = "hubId";

    private AdminHubDetailViewModel viewModel;
    private AdminHubDetailInventoryAdapter inventoryAdapter;
    private UUID hubId;

    @Nullable
    @Override
    protected FragmentAdminHubDetailBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAdminHubDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AdminHubDetailViewModel.class);
        inventoryAdapter = new AdminHubDetailInventoryAdapter();

        binding.recyclerAdminHubInventory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerAdminHubInventory.setAdapter(inventoryAdapter);

        binding.buttonAdminHubDetailBack.setOnClickListener(v -> popBackStackSafely());
        binding.buttonAddInventory.setVisibility(View.GONE);
        binding.buttonAdminHubDetailRetry.setOnClickListener(v -> fetchHubDetail());

        hubId = resolveHubIdArg();
        if (hubId == null) {
            renderError(getString(R.string.admin_hub_detail_error_invalid_hub_id));
            return;
        }

        fetchHubDetail();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getHubDetailResult().observe(getViewLifecycleOwner(),
                resultObserver(this::renderHubDetail, this::renderError));
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        if (binding == null) {
            return;
        }
        binding.progressAdminHubDetail.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            binding.scrollAdminHubDetail.setVisibility(View.GONE);
            binding.layoutAdminHubDetailError.setVisibility(View.GONE);
        }
    }

    private void fetchHubDetail() {
        if (hubId != null) {
            viewModel.fetchHubDetail(hubId);
        }
    }

    private void renderHubDetail(@Nullable Hub hub) {
        if (hub == null) {
            renderError(getString(R.string.admin_hub_detail_error_generic));
            return;
        }

        binding.scrollAdminHubDetail.setVisibility(View.VISIBLE);
        binding.layoutAdminHubDetailError.setVisibility(View.GONE);

        bindHubInfo(hub);

        List<Hub.InventoryGroup> groups = hub.getInventoryGroups() != null
                ? hub.getInventoryGroups()
                : Collections.emptyList();
        inventoryAdapter.submitList(groups);
        boolean emptyInventory = groups.isEmpty();
        binding.recyclerAdminHubInventory.setVisibility(emptyInventory ? View.GONE : View.VISIBLE);
        binding.textAdminHubDetailInventoryEmpty.setVisibility(emptyInventory ? View.VISIBLE : View.GONE);
    }

    private void bindHubInfo(@NonNull Hub hub) {
        binding.textAdminHubDetailHeaderTitle.setText(resolveHubName(hub.getName()));
        binding.textAdminHubDetailName.setText(resolveHubName(hub.getName()));
        binding.textAdminHubDetailAddress.setText(getString(
                R.string.admin_hub_detail_address_format,
                resolveAddress(hub.getAddress())));
        binding.textAdminHubDetailOperatingHours.setText(getString(
                R.string.admin_hub_detail_operating_hours_format,
                resolveOperatingHours(hub.getOperatingHours())));
        binding.textAdminHubDetailManager.setText(R.string.admin_hub_detail_manager_unassigned);
        binding.textAdminHubDetailPhone.setText(getString(
                R.string.admin_hub_detail_phone_format,
                resolvePhone(hub.getPhoneNumber())));
        binding.textAdminHubDetailLocation.setText(resolveLocationText(hub.getLocation()));
        binding.textAdminHubDetailTotalImportValue.setText(getString(
                R.string.admin_hub_detail_stat_total_import_value,
                hub.getTotalImportedQuantity()));
        binding.textAdminHubDetailReliefRunsValue.setText(getString(
                R.string.admin_hub_detail_stat_relief_runs_value,
                hub.getTotalExportedQuantity()));

        applyStatus(hub.getStatus());

        Glide.with(binding.imageAdminHubInfoThumbnail)
                .load(hub.getImageUrl())
                .placeholder(R.drawable.ic_hub_placeholder)
                .error(R.drawable.ic_hub_placeholder)
                .centerCrop()
                .into(binding.imageAdminHubInfoThumbnail);
    }

    private void applyStatus(String statusRaw) {
        HubStatus status = HubStatus.fromStringSafe(statusRaw);
        if (status == HubStatus.ACTIVE) {
            binding.textAdminHubDetailStatus.setText(R.string.admin_hub_detail_status_active);
            binding.textAdminHubDetailStatus.setBackgroundResource(R.drawable.bg_admin_detail_badge_active);
            binding.textAdminHubDetailStatus
                    .setTextColor(requireContext().getColor(R.color.admin_detail_badge_active_text));
            return;
        }

        if (status == HubStatus.EMERGENCY) {
            binding.textAdminHubDetailStatus.setText(R.string.admin_hub_detail_status_emergency);
        } else {
            binding.textAdminHubDetailStatus.setText(R.string.admin_hub_detail_status_inactive);
        }
        binding.textAdminHubDetailStatus.setBackgroundResource(R.drawable.bg_admin_badge_suspended);
        binding.textAdminHubDetailStatus.setTextColor(requireContext().getColor(R.color.admin_badge_suspended_text));
    }

    @Nullable
    private UUID resolveHubIdArg() {
        Bundle args = getArguments();
        if (args == null) {
            return null;
        }

        String rawHubId = args.getString(ARG_HUB_ID);
        if (rawHubId == null || rawHubId.trim().isEmpty()) {
            return null;
        }

        try {
            return UUID.fromString(rawHubId.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void renderError(@NonNull String message) {
        inventoryAdapter.submitList(Collections.emptyList());
        binding.scrollAdminHubDetail.setVisibility(View.GONE);
        binding.layoutAdminHubDetailError.setVisibility(View.VISIBLE);
        binding.textAdminHubDetailError.setText(message.trim().isEmpty()
                ? getString(R.string.admin_hub_detail_error_generic)
                : message);
    }

    @NonNull
    private String resolveHubName(String hubName) {
        if (hubName == null || hubName.trim().isEmpty()) {
            return getString(R.string.admin_hub_detail_name_fallback);
        }
        return hubName.trim();
    }

    @NonNull
    private String resolveAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return getString(R.string.admin_hub_detail_address_fallback);
        }
        return address.trim();
    }

    @NonNull
    private String resolveOperatingHours(String operatingHours) {
        if (operatingHours == null || operatingHours.trim().isEmpty()) {
            return getString(R.string.admin_hub_detail_operating_hours_empty);
        }
        return operatingHours.trim();
    }

    @NonNull
    private String resolvePhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return getString(R.string.admin_hub_detail_phone_empty_value);
        }
        return phoneNumber.trim();
    }

    @NonNull
    private String resolveLocationText(@Nullable Hub.Location location) {
        if (location == null || location.getLat() == null || location.getLng() == null) {
            return getString(R.string.admin_hub_detail_location_empty);
        }
        return getString(
                R.string.admin_hub_detail_location_format,
                location.getLat(),
                location.getLng());
    }
}
