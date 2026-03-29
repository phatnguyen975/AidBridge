package com.drc.aidbridge.ui.main.fragment.staff;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentStaffImportDetailBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffImportDetailFragment extends BaseFragment<FragmentStaffImportDetailBinding> {

    @Nullable
    @Override
    protected FragmentStaffImportDetailBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffImportDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
