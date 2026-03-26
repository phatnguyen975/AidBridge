package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentSponsorDonateBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorDonateFragment extends BaseFragment<FragmentSponsorDonateBinding> {

    @Nullable
    @Override
    protected FragmentSponsorDonateBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorDonateBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        binding.ivBack.setOnClickListener(v -> {
            NavController navController = getViewNavController();
            if (navController != null) {
                navController.popBackStack();
            }
        });

        setupCategoryDropdown();

        binding.cardImageUpload.setOnClickListener(v -> {
            showToast("Mở Gallery");
            simulateImageUploadPreview();
        });

        binding.tilExpectedTime.setEndIconOnClickListener(v ->
                showToast("Mở DatePicker"));

        binding.btnSubmitDonate.setOnClickListener(v -> {
            showToast("Đang xử lý...");
            navigateToSponsorHubSelectionBottomSheet();
        });
    }

    @Override
    protected void observeViewModel() {
        // TODO: Replace with ViewModel observers.
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

    private void navigateToSponsorHubSelectionBottomSheet() {
        int destinationId = requireContext().getResources().getIdentifier(
                "sponsorHubSelectionBottomSheet",
                "id",
                requireContext().getPackageName()
        );

        if (destinationId != 0) {
            navigateToDestinationSafely(destinationId);
        }
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
