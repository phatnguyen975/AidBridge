package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationItem;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationSubmissionResult;
import com.drc.aidbridge.domain.model.victim.VictimSupplyCategory;
import com.drc.aidbridge.databinding.FragmentSponsorDonateBinding;
import com.drc.aidbridge.ui.main.adapter.sponsor.SponsorHubSuggestionAdapter;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.sponsor.SponsorDonateViewModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorDonateFragment extends BaseFragment<FragmentSponsorDonateBinding> {

    private SponsorDonateViewModel viewModel;
    private final List<Hub> availableHubs = new ArrayList<>();
    private final List<VictimSupplyCategory> parentCategories = new ArrayList<>();
    private final Map<String, String> selectedLeafCategoryMap = new LinkedHashMap<>();
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

        viewModel.loadAvailableHubs();
        viewModel.loadParentCategories();
        setupHubSelectionResultListener();

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
                renderRootCategoryCheckboxes(parentCategories);
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

    private void handleSubmitDonationSuccess(@Nullable SponsorDonationSubmissionResult data) {
        String safeMessage = data != null && data.getMessage() != null && !data.getMessage().trim().isEmpty()
            ? data.getMessage().trim()
            : getString(R.string.sponsor_donate_submit_success_default);

        showTopSnackbar(binding.getRoot(), safeMessage, false);

        if (data == null) {
            navigateToDestinationSafely(R.id.sponsorQrCodeFragment);
            return;
        }

        String itemName = buildSelectedItemSummary();
        String quantityText = getString(R.string.sponsor_donate_selected_count_value, selectedLeafCategoryMap.size());

        Bundle args = new Bundle();
        args.putString(SponsorQrCodeFragment.ARG_DONATION_CODE, data.getDonationCode());
        args.putString(SponsorQrCodeFragment.ARG_QR_CODE_TOKEN, data.getQrCodeToken());
        args.putString(SponsorQrCodeFragment.ARG_ITEM_NAME, itemName);
        args.putString(SponsorQrCodeFragment.ARG_QUANTITY_TEXT, quantityText);
        navigateSafely(R.id.action_sponsor_donate_to_qr, args);
    }

    private void showSubmitError(@NonNull String message) {
        showTopSnackbar(binding.getRoot(), message, true);
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
                    buildSelectedDonationItems()
                );
            }
        );
    }

    private void renderRootCategoryCheckboxes(@NonNull List<VictimSupplyCategory> categories) {
        binding.llCategoryCheckboxContainer.removeAllViews();

        if (categories.isEmpty()) {
            TextView emptyView = new TextView(requireContext());
            emptyView.setText(getString(R.string.sponsor_hub_empty));
            emptyView.setTextColor(requireContext().getColor(R.color.text_secondary));
            binding.llCategoryCheckboxContainer.addView(emptyView);
            return;
        }

        for (VictimSupplyCategory category : categories) {
            if (category == null) {
                continue;
            }

            String categoryId = safeText(category.getId());
            String categoryName = safeText(category.getName());
            if (categoryId.isEmpty() || categoryName.isEmpty()) {
                continue;
            }

            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setText(categoryName);
            checkBox.setChecked(selectedLeafCategoryMap.containsKey(categoryId));

            LinearLayout.LayoutParams checkBoxLayoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            checkBoxLayoutParams.topMargin = getResources().getDimensionPixelSize(R.dimen.spacing_xs);
            checkBox.setLayoutParams(checkBoxLayoutParams);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedLeafCategoryMap.put(categoryId, categoryName);
                } else {
                    selectedLeafCategoryMap.remove(categoryId);
                }
            });

            binding.llCategoryCheckboxContainer.addView(checkBox);
        }
    }

    @NonNull
    private List<SponsorDonationItem> buildSelectedDonationItems() {
        List<SponsorDonationItem> items = new ArrayList<>();
        for (Map.Entry<String, String> entry : selectedLeafCategoryMap.entrySet()) {
            items.add(new SponsorDonationItem(entry.getKey(), entry.getValue()));
        }
        return items;
    }

    @NonNull
    private String buildSelectedItemSummary() {
        if (selectedLeafCategoryMap.isEmpty()) {
            return getString(R.string.sponsor_qr_missing_value);
        }

        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (String value : selectedLeafCategoryMap.values()) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(value);
            if (builder.length() > 120) {
                builder.append("...");
                break;
            }
            index++;
        }
        return builder.toString();
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
}
