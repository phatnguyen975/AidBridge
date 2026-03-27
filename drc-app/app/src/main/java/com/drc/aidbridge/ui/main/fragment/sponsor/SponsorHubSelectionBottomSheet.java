package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentSponsorHubSelectionBinding;
import com.drc.aidbridge.ui.main.adapter.sponsor.SponsorHubSuggestionAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * SponsorHubSelectionBottomSheet lets sponsor choose a hub and proceed to QR screen.
 */
public class SponsorHubSelectionBottomSheet extends BottomSheetDialogFragment {

    private FragmentSponsorHubSelectionBinding binding;
    private SponsorHubSuggestionAdapter hubSuggestionAdapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setDismissWithAnimation(true);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSponsorHubSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupHubList();
        setupActions();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (!(dialog instanceof BottomSheetDialog)) {
            return;
        }

        BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
        FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }

        int screenHeight = requireContext().getResources().getDisplayMetrics().heightPixels;
        int targetHeight = (int) (screenHeight * 0.7f);

        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
        params.height = targetHeight;
        bottomSheet.setLayoutParams(params);

        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        hubSuggestionAdapter = null;
    }

    private void setupHubList() {
        hubSuggestionAdapter = new SponsorHubSuggestionAdapter();
        binding.rvHubs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHubs.setAdapter(hubSuggestionAdapter);
        hubSuggestionAdapter.submitItems(buildMockHubSuggestions());
    }

    private void setupActions() {
        binding.btnConfirmGenerateQr.setOnClickListener(v -> navigateToQrCode());
    }

    private void navigateToQrCode() {
        int actionId = requireContext().getResources().getIdentifier(
                "action_sponsor_donate_to_qr",
                "id",
                requireContext().getPackageName()
        );

        int destinationId = requireContext().getResources().getIdentifier(
                "sponsorQrCodeFragment",
                "id",
                requireContext().getPackageName()
        );

        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.main_nav_host);
            boolean navigated = false;

            if (actionId != 0
                    && navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getAction(actionId) != null) {
                navController.navigate(actionId);
                navigated = true;
            } else if (destinationId != 0 && navController.getGraph().findNode(destinationId) != null) {
                navController.navigate(destinationId);
                navigated = true;
            }

            if (navigated) {
                dismiss();
            }
        } catch (IllegalStateException ignored) {
            // No-op: host controller is not ready.
        }
    }

    @NonNull
    private List<SponsorHubSuggestionAdapter.HubSuggestionItem> buildMockHubSuggestions() {
        List<SponsorHubSuggestionAdapter.HubSuggestionItem> items = new ArrayList<>();
        items.add(new SponsorHubSuggestionAdapter.HubSuggestionItem(
                getString(R.string.sponsor_hub_name_1),
                getString(R.string.sponsor_hub_distance_1),
                true
        ));
        items.add(new SponsorHubSuggestionAdapter.HubSuggestionItem(
                getString(R.string.sponsor_hub_name_2),
                getString(R.string.sponsor_hub_distance_2),
                false
        ));
        items.add(new SponsorHubSuggestionAdapter.HubSuggestionItem(
                getString(R.string.sponsor_hub_name_3),
                getString(R.string.sponsor_hub_distance_3),
                true
        ));
        return items;
    }
}
