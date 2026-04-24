package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;

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

    public static final String TAG = SponsorHubSelectionBottomSheet.class.getSimpleName();
    public static final String RESULT_KEY = "sponsor_hub_selection_result";
    public static final String RESULT_HUB_ID = "selected_hub_id";

    private static final String ARG_HUB_IDS = "arg_hub_ids";
    private static final String ARG_HUB_NAMES = "arg_hub_names";
    private static final String ARG_HUB_SUBTITLES = "arg_hub_subtitles";
    private static final String ARG_HUB_URGENCY = "arg_hub_urgency";

    private FragmentSponsorHubSelectionBinding binding;
    private SponsorHubSuggestionAdapter hubSuggestionAdapter;

    @NonNull
    public static SponsorHubSelectionBottomSheet newInstance(@NonNull List<SponsorHubSuggestionAdapter.HubSuggestionItem> items) {
        SponsorHubSelectionBottomSheet sheet = new SponsorHubSelectionBottomSheet();

        ArrayList<String> hubIds = new ArrayList<>();
        ArrayList<String> hubNames = new ArrayList<>();
        ArrayList<String> hubSubtitles = new ArrayList<>();
        ArrayList<Integer> urgencyFlags = new ArrayList<>();

        for (SponsorHubSuggestionAdapter.HubSuggestionItem item : items) {
            if (item == null) {
                continue;
            }

            hubIds.add(item.hubId);
            hubNames.add(item.hubName);
            hubSubtitles.add(item.distanceText);
            urgencyFlags.add(item.showUrgency ? 1 : 0);
        }

        Bundle args = new Bundle();
        args.putStringArrayList(ARG_HUB_IDS, hubIds);
        args.putStringArrayList(ARG_HUB_NAMES, hubNames);
        args.putStringArrayList(ARG_HUB_SUBTITLES, hubSubtitles);
        args.putIntegerArrayList(ARG_HUB_URGENCY, urgencyFlags);
        sheet.setArguments(args);
        return sheet;
    }

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
        hubSuggestionAdapter.submitItems(readHubSuggestionsFromArguments());
    }

    private void setupActions() {
        binding.btnConfirmGenerateQr.setOnClickListener(v -> dispatchSelectedHubAndClose());
    }

    private void dispatchSelectedHubAndClose() {
        SponsorHubSuggestionAdapter.HubSuggestionItem selectedItem = hubSuggestionAdapter.getSelectedItem();
        if (selectedItem.hubId == null || selectedItem.hubId.trim().isEmpty()) {
            return;
        }

        Bundle result = new Bundle();
        result.putString(RESULT_HUB_ID, selectedItem.hubId);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    @NonNull
    private List<SponsorHubSuggestionAdapter.HubSuggestionItem> readHubSuggestionsFromArguments() {
        List<SponsorHubSuggestionAdapter.HubSuggestionItem> items = new ArrayList<>();
        Pair<ArrayList<String>, ArrayList<String>> idNamePair = readStringLists(ARG_HUB_IDS, ARG_HUB_NAMES);
        ArrayList<String> hubIds = idNamePair.first;
        ArrayList<String> hubNames = idNamePair.second;

        ArrayList<String> hubSubtitles = getArguments() != null
                ? getArguments().getStringArrayList(ARG_HUB_SUBTITLES)
                : new ArrayList<>();

        ArrayList<Integer> urgencyFlags = getArguments() != null
                ? getArguments().getIntegerArrayList(ARG_HUB_URGENCY)
                : new ArrayList<>();

        int size = Math.min(hubIds.size(), hubNames.size());
        for (int i = 0; i < size; i++) {
            String subtitle = i < hubSubtitles.size() ? safeText(hubSubtitles.get(i)) : "";
            boolean showUrgency = i < urgencyFlags.size() && urgencyFlags.get(i) != null && urgencyFlags.get(i) == 1;
            items.add(new SponsorHubSuggestionAdapter.HubSuggestionItem(
                    safeText(hubIds.get(i)),
                    safeText(hubNames.get(i)),
                    subtitle,
                    showUrgency
            ));
        }

        return items;
    }

    @NonNull
    private Pair<ArrayList<String>, ArrayList<String>> readStringLists(String keyA, String keyB) {
        ArrayList<String> listA = getArguments() != null ? getArguments().getStringArrayList(keyA) : null;
        ArrayList<String> listB = getArguments() != null ? getArguments().getStringArrayList(keyB) : null;
        if (listA == null) {
            listA = new ArrayList<>();
        }
        if (listB == null) {
            listB = new ArrayList<>();
        }
        return new Pair<>(listA, listB);
    }

    @NonNull
    private String safeText(@Nullable String value) {
        return value != null ? value.trim() : "";
    }
}
