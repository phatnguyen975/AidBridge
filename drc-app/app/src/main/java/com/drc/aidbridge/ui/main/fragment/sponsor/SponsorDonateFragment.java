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

import com.drc.aidbridge.R;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.databinding.FragmentSponsorDonateBinding;
import com.drc.aidbridge.ui.main.adapter.sponsor.SponsorHubSuggestionAdapter;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.sponsor.SponsorDonateViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorDonateFragment extends BaseFragment<FragmentSponsorDonateBinding> {

    private SponsorDonateViewModel viewModel;
    private final List<Hub> availableHubs = new ArrayList<>();
    private boolean isHubsLoading;

    @Nullable
    @Override
    protected FragmentSponsorDonateBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorDonateBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(SponsorDonateViewModel.class);

        binding.ivBack.setOnClickListener(v -> popBackStackSafely());

        setupCategoryDropdown();
        viewModel.loadAvailableHubs();
        setupHubSelectionResultListener();

        binding.cardImageUpload.setOnClickListener(v -> {
            showToast(getString(R.string.sponsor_donate_open_gallery));
            simulateImageUploadPreview();
        });

        binding.tilExpectedTime.setEndIconOnClickListener(v ->
                showToast(getString(R.string.sponsor_donate_open_date_picker)));

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

    private void setupCategoryDropdown() {
        String[] categories = new String[] {
                getString(R.string.sponsor_donate_category_food),
                getString(R.string.sponsor_donate_category_water),
                getString(R.string.sponsor_donate_category_medicine),
                getString(R.string.sponsor_donate_category_clothes)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_sponsor_dropdown_option,
                categories
        );
        adapter.setDropDownViewResource(R.layout.item_sponsor_dropdown_option);

        binding.tvCategoryDropdown.setAdapter(adapter);
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
                    String.valueOf(binding.tvCategoryDropdown.getText()),
                    getTextSafely(binding.etItemName),
                    getTextSafely(binding.etQuantity),
                    getTextSafely(binding.etUnit),
                    getTextSafely(binding.etDescription),
                    getTextSafely(binding.etExpectedTime)
                );
            }
        );
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
