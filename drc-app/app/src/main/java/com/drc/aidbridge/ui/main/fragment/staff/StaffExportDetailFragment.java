package com.drc.aidbridge.ui.main.fragment.staff;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.FragmentStaffExportDetailBinding;
import com.drc.aidbridge.domain.model.staff.InventoryConfirmItem;
import com.drc.aidbridge.domain.model.staff.InventoryQrPreview;
import com.drc.aidbridge.domain.model.staff.StaffInventoryItem;
import com.drc.aidbridge.domain.model.staff.InventoryTransactionResult;
import com.drc.aidbridge.domain.model.staff.OutboundAidRequestDetail;
import com.drc.aidbridge.domain.model.staff.OutboundAidRequestItem;
import com.drc.aidbridge.domain.model.staff.StaffInventory;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.staff.StaffExportDetailAdapter;
import com.drc.aidbridge.ui.main.viewmodel.staff.StaffInventoryTransactionViewModel;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffExportDetailFragment extends BaseFragment<FragmentStaffExportDetailBinding> {

    private static final String ARG_CODE = "code";
    private static final String MODE_EXPORT = StaffInventoryTransactionViewModel.MODE_EXPORT;

    private StaffInventoryTransactionViewModel viewModel;
    private StaffExportDetailAdapter adapter;
    private InventoryQrPreview currentPreview;
    private String code = "";

    @Nullable
    @Override
    protected FragmentStaffExportDetailBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffExportDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(StaffInventoryTransactionViewModel.class);
        code = resolveCode();
        setupToolbar();
        setupRecycler();
        setupActions();
        hideUnavailableMockRows();
        renderInitialHeader();

        if (code.isEmpty()) {
            renderPreviewError("Vui l\u00f2ng nh\u1eadp m\u00e3.");
            return;
        }
        viewModel.preview(MODE_EXPORT, code);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getPreviewResult().observe(getViewLifecycleOwner(), this::renderPreviewResult);
        viewModel.getExportInventoryResult().observe(getViewLifecycleOwner(), this::renderExportInventoryResult);
        viewModel.getConfirmResult().observe(getViewLifecycleOwner(), this::renderConfirmResult);
    }

    private void setupToolbar() {
        binding.ivBack.setOnClickListener(v -> popBackStackSafely());
    }

    private void setupRecycler() {
        adapter = new StaffExportDetailAdapter();
        binding.rvItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvItems.setAdapter(adapter);
    }

    private void setupActions() {
        binding.btnConfirm.setEnabled(false);
        binding.btnConfirm.setOnClickListener(v -> confirmOutbound());
    }

    private void hideUnavailableMockRows() {
        binding.layoutVolunteerInfo.setVisibility(View.GONE);
        binding.layoutDestinationInfo.setVisibility(View.GONE);
        binding.dividerPartner.setVisibility(View.GONE);
    }

    private void renderInitialHeader() {
        binding.tvTaskId.setText(code.isEmpty() ? "--" : code);
        binding.tvStatus.setText(R.string.staff_detail_processing);
        binding.tvHubName.setText(R.string.staff_inventory_hub_unknown);
        binding.tvAidPeople.setText("");
        binding.tvAidDescription.setText("");
        binding.tvTotalValue.setText(getString(R.string.staff_detail_total_value_format, 0));
        binding.tvItemCount.setText("0");
        binding.tvTimeValue.setText(getString(R.string.staff_detail_time_now));
    }

    private void renderPreviewResult(@Nullable NetworkResultWrapper<InventoryQrPreview> result) {
        if (result == null) {
            renderPreviewError(getString(R.string.error_generic));
            return;
        }

        if (result.isLoading()) {
            binding.progressPagination.setVisibility(View.VISIBLE);
            binding.btnConfirm.setEnabled(false);
            return;
        }

        binding.progressPagination.setVisibility(View.GONE);
        if (result.isError()) {
            renderPreviewError(result.getMessage());
            return;
        }

        currentPreview = result.getData();
        renderPreview(currentPreview);
    }

    private void renderPreview(@Nullable InventoryQrPreview preview) {
        if (preview == null) {
            renderPreviewError(getString(R.string.error_generic));
            return;
        }

        String displayCode = !preview.getDisplayCode().isEmpty() ? preview.getDisplayCode() : code;
        binding.tvTaskId.setText(displayCode);
        binding.tvStatus.setText(preview.getStatus().isEmpty() ? "PENDING" : preview.getStatus());
        binding.tvHubName.setText(preview.getHubName().isEmpty()
                ? getString(R.string.staff_inventory_hub_unknown)
                : preview.getHubName());
        renderAidRequestDetail(preview.getAidRequestDetail());
        binding.tvItemCount.setText("0");
        binding.tvTotalValue.setText(getString(R.string.staff_detail_total_value_format, 0));
        adapter.clear();
        viewModel.loadExportInventory();
    }

    private void renderExportInventoryResult(@Nullable NetworkResultWrapper<StaffInventory> result) {
        if (result == null) {
            return;
        }

        if (result.isLoading()) {
            binding.progressPagination.setVisibility(View.VISIBLE);
            binding.btnConfirm.setEnabled(false);
            return;
        }

        binding.progressPagination.setVisibility(View.GONE);
        if (result.isError()) {
            adapter.clear();
            binding.btnConfirm.setEnabled(false);
            showTopSnackbar(binding.getRoot(), friendlyError(result.getMessage()), true);
            return;
        }

        StaffInventory inventory = result.getData();
        List<StaffInventoryItem> inventoryItems = inventory != null ? inventory.getItems() : null;
        adapter.submitInventoryItems(inventoryItems);
        binding.tvItemCount.setText(String.valueOf(adapter.getDataCount()));

        boolean canConfirm = currentPreview != null && !adapter.hasInsufficientStock() && adapter.getDataCount() > 0;
        binding.btnConfirm.setEnabled(canConfirm);
        if (!canConfirm && currentPreview != null) {
            String message = !currentPreview.getMessage().isEmpty()
                    ? currentPreview.getMessage()
                    : getString(R.string.staff_export_stock_warning);
            showTopSnackbar(binding.getRoot(), message, true);
        }
    }

    private void renderAidRequestDetail(@Nullable OutboundAidRequestDetail detail) {
        if (detail == null) {
            binding.tvAidPeople.setText("");
            binding.tvAidDescription.setText("");
            return;
        }

        binding.tvAidPeople.setText(getString(
                R.string.staff_detail_people_format,
                detail.getNumberAdult(),
                detail.getNumberElderly(),
                detail.getNumberChildren()
        ));

        String description = detail.getDescription().isEmpty()
                ? getString(R.string.staff_detail_no_description)
                : detail.getDescription();

        String requestedItemsText = buildRequestedItemsText(detail.getItems());
        binding.tvAidDescription.setText(description + "\n\n" + requestedItemsText);
    }

    private String buildRequestedItemsText(@Nullable List<OutboundAidRequestItem> requestedItems) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            return "Danh sach vat pham yeu cau: Khong co";
        }

        StringBuilder builder = new StringBuilder("Danh sach vat pham yeu cau:");
        int index = 1;
        for (OutboundAidRequestItem item : requestedItems) {
            if (item == null || item.getName().isEmpty()) {
                continue;
            }
            builder.append("\n")
                    .append(index)
                    .append(". ")
                    .append(item.getName());
            index++;
        }

        if (index == 1) {
            return "Danh sach vat pham yeu cau: Khong co";
        }
        return builder.toString();
    }

    private void renderPreviewError(@Nullable String message) {
        currentPreview = null;
        adapter.clear();
        binding.progressPagination.setVisibility(View.GONE);
        binding.btnConfirm.setEnabled(false);
        showTopSnackbar(binding.getRoot(), friendlyError(message), true);
    }

    private void confirmOutbound() {
        if (adapter.hasInsufficientStock()) {
            showTopSnackbar(binding.getRoot(), getString(R.string.staff_export_over_stock), true);
            return;
        }

        List<InventoryConfirmItem> items = adapter.getConfirmItems();
        if (items.isEmpty()) {
            showTopSnackbar(binding.getRoot(), getString(R.string.staff_export_empty_quantity), true);
            return;
        }

        binding.btnConfirm.setEnabled(false);
        viewModel.confirm(MODE_EXPORT, code, items, getString(R.string.staff_export_note_default));
    }

    private void renderConfirmResult(@Nullable NetworkResultWrapper<InventoryTransactionResult> result) {
        if (result == null) {
            return;
        }
        if (result.isLoading()) {
            binding.progressPagination.setVisibility(View.VISIBLE);
            binding.btnConfirm.setEnabled(false);
            return;
        }

        binding.progressPagination.setVisibility(View.GONE);
        if (result.isError()) {
            binding.btnConfirm.setEnabled(currentPreview != null
                    && currentPreview.canConfirm()
                    && !adapter.hasInsufficientStock());
            showTopSnackbar(binding.getRoot(), friendlyError(result.getMessage()), true);
            return;
        }

        InventoryTransactionResult transactionResult = result.getData();
        String message = transactionResult != null && !transactionResult.getMessage().isEmpty()
                ? transactionResult.getMessage()
                : "Xu\u1ea5t kho th\u00e0nh c\u00f4ng.";
        showTopSnackbar(binding.getRoot(), message, false);
        binding.getRoot().postDelayed(
                () -> popBackStackSafely(R.id.staffInventoryFragment, false),
                700L
        );
    }

    private String resolveCode() {
        Bundle args = getArguments();
        String argCode = args != null ? args.getString(ARG_CODE, "") : "";
        return argCode != null ? argCode.trim() : "";
    }

    private String friendlyError(@Nullable String message) {
        String safe = message != null ? message.trim() : "";
        return safe.isEmpty() ? getString(R.string.error_generic) : safe;
    }
}
