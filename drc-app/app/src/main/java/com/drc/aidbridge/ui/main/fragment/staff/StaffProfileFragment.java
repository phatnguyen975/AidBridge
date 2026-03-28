package com.drc.aidbridge.ui.main.fragment.staff;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentStaffProfileBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffProfileFragment extends BaseFragment<FragmentStaffProfileBinding> {

    @Nullable
    @Override
    protected FragmentStaffProfileBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
