package com.drc.aidbridge.ui.main.fragment.staff;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentStaffScannerBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffScannerFragment extends BaseFragment<FragmentStaffScannerBinding> {

    private static final String ARG_MODE = "mode";
    private static final String MODE_IMPORT = "import";
    private static final String MODE_EXPORT = "export";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String mode = MODE_EXPORT;

    @Nullable
    @Override
    protected FragmentStaffScannerBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffScannerBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        mode = resolveMode();
        bindModeUi();

        binding.ivBack.setOnClickListener(v -> popBackStackSafely());
        binding.btnManualEntry.setOnClickListener(v -> showManualEntryBottomSheet());

        // TODO: MOCK SCAN DELAY - REMOVE LATER when integrating actual Camera SDK
        handler.postDelayed(() -> {
            if (!isAdded() || getView() == null || !isVisible()) {
                return;
            }
            if (MODE_IMPORT.equals(mode)) {
                navigateToDestinationSafely(R.id.staffImportDetailFragment);
            } else {
                navigateToDestinationSafely(R.id.staffExportDetailFragment);
            }
        }, 8000L);
    }

    @Override
    protected void observeViewModel() {
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }

    private String resolveMode() {
        Bundle args = getArguments();
        String argMode = args != null ? args.getString(ARG_MODE, MODE_EXPORT) : MODE_EXPORT;
        return MODE_IMPORT.equals(argMode) ? MODE_IMPORT : MODE_EXPORT;
    }

    private void bindModeUi() {
        if (MODE_IMPORT.equals(mode)) {
            binding.tvToolbarTitle.setText(R.string.staff_scan_title_import);
            binding.tvInstruction.setText(R.string.staff_scan_inst_import);
            return;
        }
        binding.tvToolbarTitle.setText(R.string.staff_scan_title_export);
        binding.tvInstruction.setText(R.string.staff_scan_inst_export);
    }

    private void showManualEntryBottomSheet() {
        StaffManualEntryBottomSheet.newInstance(mode)
                .show(getChildFragmentManager(), StaffManualEntryBottomSheet.class.getSimpleName());
    }
}
