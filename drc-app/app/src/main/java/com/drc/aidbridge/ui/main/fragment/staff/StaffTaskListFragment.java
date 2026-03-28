package com.drc.aidbridge.ui.main.fragment.staff;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentStaffTaskListBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffTaskListFragment extends BaseFragment<FragmentStaffTaskListBinding> {

    @Nullable
    @Override
    protected FragmentStaffTaskListBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffTaskListBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
