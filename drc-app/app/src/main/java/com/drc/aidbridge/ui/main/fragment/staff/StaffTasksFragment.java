package com.drc.aidbridge.ui.main.fragment.staff;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentStaffTasksBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffTasksFragment extends BaseFragment<FragmentStaffTasksBinding> {

    @Nullable
    @Override
    protected FragmentStaffTasksBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffTasksBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
