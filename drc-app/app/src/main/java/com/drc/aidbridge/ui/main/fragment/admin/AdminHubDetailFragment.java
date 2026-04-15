package com.drc.aidbridge.ui.main.fragment.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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

import java.util.ArrayList;
import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminHubDetailFragment extends BaseFragment<FragmentAdminHubDetailBinding> {

    public static final String ARG_HUB_ID = "hubId";
    private static final String TAG = "AdminHubDetailFragment";

    private AdminHubDetailViewModel viewModel;
    private AdminHubDetailInventoryAdapter inventoryAdapter;

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

        setupClickListeners();

        UUID hubId = resolveHubIdArg();
        if (hubId == null) {
            showToast(getString(R.string.admin_hub_detail_error_invalid_hub_id));
            return;
        }

        viewModel.fetchHubDetail(hubId);
    }

    private void setupClickListeners() {
        binding.buttonAdminHubDetailBack.setOnClickListener(v -> popBackStackSafely());
        binding.buttonAddInventory
                .setOnClickListener(v -> navigateToDestinationSafely(R.id.adminAddSupplyTypeFragment));
    }

    @Override
    protected void observeViewModel() {
        viewModel.getHubDetailResult().observe(getViewLifecycleOwner(),
                resultObserver(this::renderHubDetail, this::showHubDetailError));
    }

    private void renderHubDetail(@Nullable Hub hub) {
        if (hub == null) {
            showHubDetailError(getString(R.string.admin_hub_detail_error_generic));
            return;
        }

        bindHubInfo(hub);
        inventoryAdapter.submitList(hub.getInventory() != null ? hub.getInventory() : new ArrayList<>());
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

        applyStatus(hub.getStatus());

        Glide.with(binding.imageAdminHubDetailThumbnail)
                .load(hub.getImageUrl())
                .placeholder(R.drawable.ic_hub_placeholder)
                .error(R.drawable.ic_hub_placeholder)
                .centerCrop()
                .into(binding.imageAdminHubDetailThumbnail);

        Hub.Location location = hub.getLocation();
        if (location != null) {
            Log.d(TAG, "Hub location lat=" + location.getLat() + ", lng=" + location.getLng());
        }
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

    private void showHubDetailError(@NonNull String message) {
        if (message.trim().isEmpty()) {
            showToast(getString(R.string.admin_hub_detail_error_generic));
            return;
        }
        showToast(message);
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
}
