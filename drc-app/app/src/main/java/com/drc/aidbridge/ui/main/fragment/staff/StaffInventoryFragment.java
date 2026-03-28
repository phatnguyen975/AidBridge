package com.drc.aidbridge.ui.main.fragment.staff;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentStaffInventoryBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffInventoryFragment extends BaseFragment<FragmentStaffInventoryBinding> {

    @Nullable
    @Override
    protected FragmentStaffInventoryBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffInventoryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
