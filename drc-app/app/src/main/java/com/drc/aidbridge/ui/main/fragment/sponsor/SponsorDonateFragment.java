package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationItem;
import com.drc.aidbridge.domain.model.victim.VictimSupplyCategory;
import com.drc.aidbridge.databinding.FragmentSponsorDonateBinding;
import com.drc.aidbridge.ui.main.adapter.sponsor.SponsorDonationItemAdapter;
import com.drc.aidbridge.ui.main.adapter.sponsor.SponsorHubSuggestionAdapter;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.sponsor.SponsorDonateViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorDonateFragment extends BaseFragment<FragmentSponsorDonateBinding> {

    private SponsorDonateViewModel viewModel;
    private SponsorDonationItemAdapter donationItemAdapter;
    private final List<Hub> availableHubs = new ArrayList<>();
    private final List<SponsorDonationItem> draftItems = new ArrayList<>();
    private final List<VictimSupplyCategory> parentCategories = new ArrayList<>();
    private boolean isHubsLoading;
    private boolean isCategoriesLoading;

    @Nullable
    @Override
    protected FragmentSponsorDonateBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorDonateBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(SponsorDonateViewModel.class);

        binding.ivBack.setOnClickListener(v -> popBackStackSafely());

        setupDonationItemsList();
        viewModel.loadAvailableHubs();
        viewModel.loadParentCategories();
        setupHubSelectionResultListener();

        binding.cardImageUpload.setOnClickListener(v -> {
            showToast(getString(R.string.sponsor_donate_open_gallery));
            simulateImageUploadPreview();
        });

        binding.btnAddDonationItem.setOnClickListener(v -> {
            if (binding.llItemFormContainer.getVisibility() != View.VISIBLE) {
                binding.llItemFormContainer.setVisibility(View.VISIBLE);
                binding.btnAddDonationItem.setText(R.string.sponsor_donate_save_item);
                return;
            }

            addCurrentDraftItem();
        });

        binding.btnSubmitDonate.setOnClickListener(v -> {
            clearInputFocusAndHideKeyboard();
            openSponsorHubSelectionBottomSheet();
        });
    }

    @Override
    protected void observeViewModel() {
        if (viewModel == null) {
            return;
        }

        viewModel.getValidationError().observe(getViewLifecycleOwner(), validationResult -> {
            if (validationResult == null || validationResult.isValid()) {
                return;
            }

            String message = validationResult.getErrorMessage();
            showTopSnackbar(
                binding.getRoot(),
                message != null && !message.trim().isEmpty() ? message : getString(R.string.error_generic),
                true
            );
        });

        viewModel.getHubsResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) {
                return;
            }

            isHubsLoading = result.isLoading();
            if (result.isLoading() || result.hasBeenHandled()) {
                return;
            }

            result.markAsHandled();
            if (result.isSuccess()) {
                List<Hub> hubs = result.getData();
                availableHubs.clear();
                if (hubs != null) {
                    availableHubs.addAll(hubs);
                }
                return;
            }

            if (result.isError()) {
                showTopSnackbar(
                    binding.getRoot(),
                    result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic),
                    true
                );
            }
        });

        viewModel.getCategoriesResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) {
                return;
            }

            isCategoriesLoading = result.isLoading();
            if (result.isLoading() || result.hasBeenHandled()) {
                return;
            }

            result.markAsHandled();
            if (result.isSuccess()) {
                parentCategories.clear();
                List<VictimSupplyCategory> categories = result.getData();
                if (categories != null) {
                    parentCategories.addAll(categories);
                }
                bindParentCategoriesDropdown(parentCategories);
                return;
            }

            if (result.isError()) {
                showTopSnackbar(
                    binding.getRoot(),
                    result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic),
                    true
                );
            }
        });

        viewModel.getSubmitResult().observe(
            getViewLifecycleOwner(),
            resultObserver(this::handleSubmitDonationSuccess, this::showSubmitError)
        );
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        binding.btnAddDonationItem.setEnabled(!isLoading);
        binding.btnSubmitDonate.setEnabled(!isLoading);
        binding.btnSubmitDonate.setText(isLoading ? R.string.btn_loading : R.string.sponsor_donate_submit);
    }

    private void openSponsorHubSelectionBottomSheet() {
        if (!isAdded()) {
            return;
        }

        if (isHubsLoading) {
            showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_hub_loading), true);
            return;
        }

        List<SponsorHubSuggestionAdapter.HubSuggestionItem> hubSuggestions = mapHubSuggestions(availableHubs);
        if (hubSuggestions.isEmpty()) {
            showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_hub_empty), true);
            return;
        }

        if (getChildFragmentManager().findFragmentByTag(SponsorHubSelectionBottomSheet.TAG) == null) {
            SponsorHubSelectionBottomSheet.newInstance(hubSuggestions)
                    .show(getChildFragmentManager(), SponsorHubSelectionBottomSheet.TAG);
        }
    }

    private void handleSubmitDonationSuccess(@Nullable String message) {
        String safeMessage = message != null && !message.trim().isEmpty()
            ? message
            : getString(R.string.sponsor_donate_submit_success_default);
        showTopSnackbar(binding.getRoot(), safeMessage, false);
        navigateToDestinationSafely(R.id.sponsorQrCodeFragment);
    }

    private void showSubmitError(@NonNull String message) {
        showTopSnackbar(binding.getRoot(), message, true);
    }

    @NonNull
    private String getTextSafely(@Nullable EditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }

        return editText.getText().toString().trim();
    }

    private void setupHubSelectionResultListener() {
        getChildFragmentManager().setFragmentResultListener(
            SponsorHubSelectionBottomSheet.RESULT_KEY,
            getViewLifecycleOwner(),
            (requestKey, result) -> {
                if (!SponsorHubSelectionBottomSheet.RESULT_KEY.equals(requestKey)) {
                    return;
                }

                String selectedHubId = result.getString(SponsorHubSelectionBottomSheet.RESULT_HUB_ID, "");
                viewModel.submitDonation(
                    selectedHubId,
                    getTextSafely(binding.etDonationNotes),
                    new ArrayList<>(draftItems)
                );
            }
        );
    }

    private void setupDonationItemsList() {
        donationItemAdapter = new SponsorDonationItemAdapter(position -> {
            if (position < 0 || position >= draftItems.size()) {
                return;
            }

            draftItems.remove(position);
            donationItemAdapter.submitItems(new ArrayList<>(draftItems));
            showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_donate_item_removed_success), false);
        });

        binding.rvDonationItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDonationItems.setAdapter(donationItemAdapter);
        donationItemAdapter.submitItems(new ArrayList<>(draftItems));
    }

    private void addCurrentDraftItem() {
        if (isCategoriesLoading) {
            showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_category_loading), true);
            return;
        }

        String itemName = getTextSafely(binding.etItemName);
        String categoryId = resolveSelectedParentCategoryId(getTextSafely(binding.tvParentCategoryDropdown));
        int quantity = parseQuantity(getTextSafely(binding.etQuantity));
        String unit = getTextSafely(binding.etUnit);
        String description = getTextSafely(binding.etDescription);
        String expiryDate = getTextSafely(binding.etExpiryDate);

        if (itemName.isEmpty()) {
            showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_donate_item_name_required), true);
            return;
        }

        if (quantity <= 0) {
            showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_donate_quantity_required), true);
            return;
        }

        if (unit.isEmpty()) {
            showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_donate_unit_required), true);
            return;
        }

        SponsorDonationItem item = new SponsorDonationItem(
            itemName,
            categoryId,
            quantity,
            unit,
            description,
            expiryDate,
            null
        );

        draftItems.add(item);
        donationItemAdapter.submitItems(new ArrayList<>(draftItems));
        clearItemInputFields();
        binding.llItemFormContainer.setVisibility(View.GONE);
        binding.btnAddDonationItem.setText(R.string.sponsor_donate_add_item);
        showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_donate_item_added_success), false);
    }

    private int parseQuantity(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void clearItemInputFields() {
        binding.etItemName.setText("");
        binding.tvParentCategoryDropdown.setText("", false);
        binding.etQuantity.setText("");
        binding.etUnit.setText("");
        binding.etDescription.setText("");
        binding.etExpiryDate.setText("");
    }

    private void bindParentCategoriesDropdown(@NonNull List<VictimSupplyCategory> categories) {
        List<String> names = new ArrayList<>();
        for (VictimSupplyCategory category : categories) {
            if (category == null) {
                continue;
            }
            String name = safeText(category.getName());
            if (!name.isEmpty()) {
                names.add(name);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            R.layout.item_sponsor_dropdown_option,
            names
        );
        adapter.setDropDownViewResource(R.layout.item_sponsor_dropdown_option);
        binding.tvParentCategoryDropdown.setAdapter(adapter);
    }

    @Nullable
    private String resolveSelectedParentCategoryId(@NonNull String selectedName) {
        if (selectedName.trim().isEmpty()) {
            return null;
        }

        for (VictimSupplyCategory category : parentCategories) {
            if (category == null) {
                continue;
            }

            if (selectedName.equalsIgnoreCase(safeText(category.getName()))) {
                String id = safeText(category.getId());
                return id.isEmpty() ? null : id;
            }
        }

        return null;
    }

    @NonNull
    private List<SponsorHubSuggestionAdapter.HubSuggestionItem> mapHubSuggestions(@Nullable List<Hub> hubs) {
        List<SponsorHubSuggestionAdapter.HubSuggestionItem> items = new ArrayList<>();
        if (hubs == null || hubs.isEmpty()) {
            return items;
        }

        for (Hub hub : hubs) {
            if (hub == null || hub.getId() == null) {
                continue;
            }

            String title = safeText(hub.getName());
            String subtitle = safeText(hub.getAddress());
            if (title.isEmpty()) {
                title = getString(R.string.sponsor_hub_unknown_name);
            }
            if (subtitle.isEmpty()) {
                subtitle = getString(R.string.sponsor_hub_unknown_address);
            }

            items.add(new SponsorHubSuggestionAdapter.HubSuggestionItem(
                hub.getId().toString(),
                title,
                subtitle,
                false
            ));
        }

        return items;
    }

    @NonNull
    private String safeText(@Nullable String text) {
        return text != null ? text.trim() : "";
    }

    private void simulateImageUploadPreview() {
        // TODO: Replace this mock method with actual photo-taking result handler
        binding.llImagePreviewContainer.setVisibility(View.VISIBLE);

        if (binding.llImagePreviewContainer.getChildCount() > 0) {
            return;
        }

        for (int i = 0; i < 2; i++) {
            @NonNull View previewView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_sponsor_donate_image_preview, binding.llImagePreviewContainer, false);

            ImageView thumbnailView = previewView.findViewById(R.id.iv_thumbnail);
            ImageButton closeButton = previewView.findViewById(R.id.iv_close);

            thumbnailView.setImageResource(R.mipmap.ic_launcher);

            closeButton.setOnClickListener(v -> {
                binding.llImagePreviewContainer.removeView(previewView);
                if (binding.llImagePreviewContainer.getChildCount() == 0) {
                    binding.llImagePreviewContainer.setVisibility(View.GONE);
                }
            });

            binding.llImagePreviewContainer.addView(previewView);
        }
    }
}
