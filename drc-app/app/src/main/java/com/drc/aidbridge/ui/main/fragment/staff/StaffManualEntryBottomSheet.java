package com.drc.aidbridge.ui.main.fragment.staff;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentStaffManualEntryBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class StaffManualEntryBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_MODE = "mode";
    private static final String MODE_IMPORT = "import";
    private static final String MODE_EXPORT = "export";

    private FragmentStaffManualEntryBinding binding;
    private String mode = MODE_EXPORT;

    public static StaffManualEntryBottomSheet newInstance(String mode) {
        StaffManualEntryBottomSheet bottomSheet = new StaffManualEntryBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        bottomSheet.setArguments(args);
        return bottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStaffManualEntryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mode = resolveMode();
        binding.btnConfirmManual.setOnClickListener(v -> {
            navigateByMode();
            dismissAllowingStateLoss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private String resolveMode() {
        Bundle arguments = getArguments();
        String argMode = arguments != null ? arguments.getString(ARG_MODE, MODE_EXPORT) : MODE_EXPORT;
        return MODE_IMPORT.equals(argMode) ? MODE_IMPORT : MODE_EXPORT;
    }

    private void navigateByMode() {
        int destinationId = MODE_IMPORT.equals(mode)
                ? R.id.staffImportDetailFragment
                : R.id.staffExportDetailFragment;

        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.main_nav_host);
            if (navController.getGraph().findNode(destinationId) != null) {
                navController.navigate(destinationId);
            }
        } catch (IllegalStateException ignored) {
            // No-op: host controller is not ready.
        }
    }
}
