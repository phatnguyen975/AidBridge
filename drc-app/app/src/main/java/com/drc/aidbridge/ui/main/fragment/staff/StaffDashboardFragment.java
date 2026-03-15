package com.drc.aidbridge.ui.main.fragment.staff;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentStaffDashboardBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffDashboardFragment extends BaseFragment<FragmentStaffDashboardBinding> {

    @Nullable
    @Override
    protected FragmentStaffDashboardBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffDashboardBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
