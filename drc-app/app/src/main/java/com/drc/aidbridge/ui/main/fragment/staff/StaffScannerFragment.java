package com.drc.aidbridge.ui.main.fragment.staff;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentStaffScannerBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffScannerFragment extends BaseFragment<FragmentStaffScannerBinding> {

    @Nullable
    @Override
    protected FragmentStaffScannerBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffScannerBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
