package com.drc.aidbridge.ui.main.fragment.staff;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentStaffExportDetailBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffExportDetailFragment extends BaseFragment<FragmentStaffExportDetailBinding> {

    @Nullable
    @Override
    protected FragmentStaffExportDetailBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffExportDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
