package com.drc.aidbridge.ui.main.fragment.staff;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.FragmentStaffImportDetailBinding;
import com.drc.aidbridge.domain.model.staff.InboundDonationPreview;
import com.drc.aidbridge.domain.model.staff.InboundDraftItem;
import com.drc.aidbridge.domain.model.staff.InboundParentCategory;
import com.drc.aidbridge.domain.model.staff.InboundSubCategory;
import com.drc.aidbridge.domain.model.staff.InventoryTransactionResult;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.staff.StaffImportDetailAdapter;
import com.drc.aidbridge.ui.main.viewmodel.staff.StaffInventoryTransactionViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffImportDetailFragment extends BaseFragment<FragmentStaffImportDetailBinding> {

    private static final String ARG_CODE = "code";

    private final List<InboundDraftItem> draftItems = new ArrayList<>();

    private StaffInventoryTransactionViewModel viewModel;
    private StaffImportDetailAdapter adapter;
    private InboundDonationPreview currentPreview;
    private String code = "";

    @Nullable
    @Override
    protected FragmentStaffImportDetailBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffImportDetailBinding.inflate(inflater, container, false);
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
        viewModel.previewInbound(code);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getInboundPreviewResult().observe(getViewLifecycleOwner(), this::renderPreviewResult);
        viewModel.getInboundConfirmResult().observe(getViewLifecycleOwner(), this::renderConfirmResult);
    }

    private void setupToolbar() {
        binding.ivBack.setOnClickListener(v -> popBackStackSafely());
    }

    private void setupRecycler() {
        adapter = new StaffImportDetailAdapter();
        binding.rvItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvItems.setAdapter(adapter);
    }

    private void setupActions() {
        binding.btnAddReceivedItem.setEnabled(false);
        binding.btnAddReceivedItem.setOnClickListener(v -> showAddDraftItemDialog());
        binding.btnConfirm.setEnabled(false);
        binding.btnConfirm.setOnClickListener(v -> confirmInbound());
    }

    private void hideUnavailableMockRows() {
        binding.layoutPartnerInfo.setVisibility(View.GONE);
        binding.dividerPartner.setVisibility(View.GONE);
    }

    private void renderInitialHeader() {
        binding.tvTaskId.setText(code.isEmpty() ? "--" : code);
        binding.tvStatus.setText(R.string.staff_detail_processing);
        binding.tvHubName.setText(R.string.staff_inventory_hub_unknown);
        binding.tvRegisteredCategories.setText("");
        binding.tvTotalValue.setText("0 v\u1eadt ph\u1ea9m");
        binding.tvItemCount.setText("0");
        binding.tvTimeValue.setText("B\u00e2y gi\u1edd");
    }

    private void renderPreviewResult(@Nullable NetworkResultWrapper<InboundDonationPreview> result) {
        if (result == null) {
            renderPreviewError(getString(R.string.error_generic));
            return;
        }

        if (result.isLoading()) {
            binding.progressPagination.setVisibility(View.VISIBLE);
            binding.btnAddReceivedItem.setEnabled(false);
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

    private void renderPreview(@Nullable InboundDonationPreview preview) {
        if (preview == null) {
            renderPreviewError(getString(R.string.error_generic));
            return;
        }

        binding.tvTaskId.setText(!preview.getDonationCode().isEmpty() ? preview.getDonationCode() : code);
        binding.tvStatus.setText(preview.getStatus().isEmpty() ? "REGISTERED" : preview.getStatus());
        binding.tvHubName.setText(preview.getHubName().isEmpty()
                ? getString(R.string.staff_inventory_hub_unknown)
                : preview.getHubName());
        binding.tvRegisteredCategories.setText(buildRegisteredCategoryText(preview));
        binding.btnAddReceivedItem.setEnabled(!preview.getRegisteredParentCategories().isEmpty());
        renderDraftItems();
    }

    private void renderPreviewError(@Nullable String message) {
        currentPreview = null;
        draftItems.clear();
        adapter.clear();
        binding.progressPagination.setVisibility(View.GONE);
        binding.btnAddReceivedItem.setEnabled(false);
        binding.btnConfirm.setEnabled(false);
        showTopSnackbar(binding.getRoot(), friendlyError(message), true);
    }

    private void showAddDraftItemDialog() {
        if (currentPreview == null || currentPreview.getRegisteredParentCategories().isEmpty()) {
            showTopSnackbar(binding.getRoot(), "Donation ch\u01b0a c\u00f3 lo\u1ea1i h\u00e0ng \u0111\u0103ng k\u00fd.", true);
            return;
        }

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen.spacing_md);
        container.setPadding(padding, padding, padding, 0);

        Spinner parentSpinner = new Spinner(requireContext());
        Spinner subCategorySpinner = new Spinner(requireContext());
        EditText quantityInput = new EditText(requireContext());
        EditText noteInput = new EditText(requireContext());

        quantityInput.setHint("S\u1ed1 l\u01b0\u1ee3ng th\u1ef1c nh\u1eadn");
        quantityInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        noteInput.setHint("Ghi ch\u00fa");

        List<InboundParentCategory> parents = currentPreview.getRegisteredParentCategories();
        ArrayAdapter<String> parentAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                parents.stream().map(InboundParentCategory::getParentCategoryName).collect(Collectors.toList())
        );
        parentSpinner.setAdapter(parentAdapter);

        List<InboundSubCategory> selectedSubCategories = new ArrayList<>();
        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new ArrayList<>()
        );
        subCategorySpinner.setAdapter(subAdapter);

        parentSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                InboundParentCategory selectedParent = parents.get(position);
                loadSubCategoriesForDialog(selectedParent, selectedSubCategories, subAdapter);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        addLabeledView(container, "Lo\u1ea1i sponsor \u0111\u0103ng k\u00fd", parentSpinner);
        addLabeledView(container, "V\u1eadt ph\u1ea9m th\u1ef1c nh\u1eadn", subCategorySpinner);
        addLabeledView(container, "S\u1ed1 l\u01b0\u1ee3ng", quantityInput);
        addLabeledView(container, "Ghi ch\u00fa", noteInput);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Th\u00eam v\u1eadt ph\u1ea9m th\u1ef1c nh\u1eadn")
                .setView(container)
                .setPositiveButton("Th\u00eam", null)
                .setNeutralButton("T\u1ea1o v\u1eadt ph\u1ea9m m\u1edbi", null)
                .setNegativeButton("H\u1ee7y", null)
                .create();

        dialog.setOnShowListener(unused -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int parentPosition = parentSpinner.getSelectedItemPosition();
                int subPosition = subCategorySpinner.getSelectedItemPosition();
                if (parentPosition < 0 || subPosition < 0 || selectedSubCategories.isEmpty()) {
                    showTopSnackbar(binding.getRoot(), "Vui l\u00f2ng ch\u1ecdn ho\u1eb7c t\u1ea1o v\u1eadt ph\u1ea9m.", true);
                    return;
                }
                int quantity = parsePositiveInt(quantityInput.getText() != null
                        ? quantityInput.getText().toString()
                        : "");
                if (quantity <= 0) {
                    showTopSnackbar(binding.getRoot(), "S\u1ed1 l\u01b0\u1ee3ng ph\u1ea3i l\u1edbn h\u01a1n 0.", true);
                    return;
                }

                InboundParentCategory selectedParent = parents.get(parentPosition);
                InboundSubCategory selectedSub = selectedSubCategories.get(subPosition);
                draftItems.add(new InboundDraftItem(
                        selectedParent.getParentCategoryId(),
                        selectedParent.getParentCategoryName(),
                        selectedSub.getItemCategoryId(),
                        selectedSub.getName(),
                        selectedSub.getUnit(),
                        quantity,
                        noteInput.getText() != null ? noteInput.getText().toString() : ""
                ));
                renderDraftItems();
                dialog.dismiss();
            });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                int parentPosition = parentSpinner.getSelectedItemPosition();
                if (parentPosition < 0) {
                    return;
                }
                InboundParentCategory selectedParent = parents.get(parentPosition);
                dialog.dismiss();
                showCreateSubCategoryDialog(selectedParent);
            });
        });

        dialog.show();
    }

    private void loadSubCategoriesForDialog(InboundParentCategory parent,
                                            List<InboundSubCategory> selectedSubCategories,
                                            ArrayAdapter<String> subAdapter) {
        selectedSubCategories.clear();
        selectedSubCategories.addAll(parent.getAvailableSubCategories());
        updateSubCategoryAdapter(selectedSubCategories, subAdapter);

        viewModel.searchInboundSubCategories(
                currentPreview.getDonationId(),
                parent.getParentCategoryId(),
                null
        ).observe(getViewLifecycleOwner(), new Observer<NetworkResultWrapper<List<InboundSubCategory>>>() {
            @Override
            public void onChanged(NetworkResultWrapper<List<InboundSubCategory>> result) {
                if (result == null || result.isLoading()) {
                    return;
                }
                if (result.isSuccess()) {
                    selectedSubCategories.clear();
                    if (result.getData() != null) {
                        selectedSubCategories.addAll(result.getData());
                    }
                    updateSubCategoryAdapter(selectedSubCategories, subAdapter);
                }
            }
        });
    }

    private void showCreateSubCategoryDialog(InboundParentCategory parent) {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen.spacing_md);
        container.setPadding(padding, padding, padding, 0);

        EditText nameInput = new EditText(requireContext());
        EditText unitInput = new EditText(requireContext());
        nameInput.setHint("T\u00ean v\u1eadt ph\u1ea9m");
        unitInput.setHint("\u0110\u01a1n v\u1ecb");
        addLabeledView(container, "T\u00ean v\u1eadt ph\u1ea9m", nameInput);
        addLabeledView(container, "\u0110\u01a1n v\u1ecb", unitInput);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("T\u1ea1o v\u1eadt ph\u1ea9m trong " + parent.getParentCategoryName())
                .setView(container)
                .setPositiveButton("T\u1ea1o", null)
                .setNegativeButton("H\u1ee7y", null)
                .create();

        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            String unit = unitInput.getText() != null ? unitInput.getText().toString().trim() : "";
            if (name.isEmpty() || unit.isEmpty()) {
                showTopSnackbar(binding.getRoot(), "T\u00ean v\u00e0 \u0111\u01a1n v\u1ecb kh\u00f4ng \u0111\u01b0\u1ee3c r\u1ed7ng.", true);
                return;
            }

            viewModel.createInboundSubCategory(
                    currentPreview.getDonationId(),
                    parent.getParentCategoryId(),
                    name,
                    unit,
                    null
            ).observe(getViewLifecycleOwner(), new Observer<NetworkResultWrapper<InboundSubCategory>>() {
                @Override
                public void onChanged(NetworkResultWrapper<InboundSubCategory> result) {
                    if (result == null || result.isLoading()) {
                        return;
                    }
                    if (result.isError()) {
                        showTopSnackbar(binding.getRoot(), friendlyError(result.getMessage()), true);
                        return;
                    }
                    InboundSubCategory created = result.getData();
                    if (created != null) {
                        parent.addSubCategory(created);
                    }
                    showTopSnackbar(binding.getRoot(), "T\u1ea1o v\u1eadt ph\u1ea9m th\u00e0nh c\u00f4ng.", false);
                    dialog.dismiss();
                    showAddDraftItemDialog();
                }
            });
        }));

        dialog.show();
    }

    private void renderDraftItems() {
        adapter.submitDraftItems(draftItems);
        binding.tvItemCount.setText(String.valueOf(draftItems.size()));
        int total = 0;
        for (InboundDraftItem item : draftItems) {
            total += item.getQuantity();
        }
        binding.tvTotalValue.setText(total + " v\u1eadt ph\u1ea9m");
        binding.btnConfirm.setEnabled(!draftItems.isEmpty());
    }

    private void confirmInbound() {
        if (currentPreview == null) {
            return;
        }
        if (draftItems.isEmpty()) {
            showTopSnackbar(binding.getRoot(), "Vui l\u00f2ng th\u00eam v\u1eadt ph\u1ea9m th\u1ef1c nh\u1eadn.", true);
            return;
        }

        binding.btnConfirm.setEnabled(false);
        viewModel.confirmInbound(
                currentPreview.getDonationId(),
                code,
                new ArrayList<>(draftItems),
                "\u0110\u00e3 ki\u1ec3m tra th\u1ef1c t\u1ebf t\u1ea1i \u0111i\u1ec3m nh\u1eadn"
        );
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
            binding.btnConfirm.setEnabled(!draftItems.isEmpty());
            showTopSnackbar(binding.getRoot(), friendlyError(result.getMessage()), true);
            return;
        }

        InventoryTransactionResult transactionResult = result.getData();
        String message = transactionResult != null && !transactionResult.getMessage().isEmpty()
                ? transactionResult.getMessage()
                : "Nh\u1eadp kho th\u00e0nh c\u00f4ng.";
        draftItems.clear();
        renderDraftItems();
        showTopSnackbar(binding.getRoot(), message, false);
        binding.getRoot().postDelayed(
                () -> popBackStackSafely(R.id.staffInventoryFragment, false),
                700L
        );
    }

    private void updateSubCategoryAdapter(List<InboundSubCategory> subCategories,
                                          ArrayAdapter<String> subAdapter) {
        subAdapter.clear();
        if (subCategories != null) {
            for (InboundSubCategory subCategory : subCategories) {
                subAdapter.add(subCategory.getName() + " (" + subCategory.getUnit() + ")");
            }
        }
        subAdapter.notifyDataSetChanged();
    }

    private void addLabeledView(LinearLayout container, String label, View view) {
        TextView labelView = new TextView(requireContext());
        labelView.setText(label);
        labelView.setTextColor(android.graphics.Color.DKGRAY);
        container.addView(labelView);
        container.addView(view);
    }

    private String buildRegisteredCategoryText(InboundDonationPreview preview) {
        String names = preview.getRegisteredParentCategories().stream()
                .map(InboundParentCategory::getParentCategoryName)
                .collect(Collectors.joining(", "));
        return "Sponsor \u0111\u00e3 \u0111\u0103ng k\u00fd: " + names;
    }

    private int parsePositiveInt(String value) {
        try {
            return Math.max(Integer.parseInt(value.trim()), 0);
        } catch (NumberFormatException ignored) {
            return 0;
        }
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
