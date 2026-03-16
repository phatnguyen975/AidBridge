package com.drc.aidbridge.ui.main.fragment.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentAdminDashboardBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminDashboardFragment extends BaseFragment<FragmentAdminDashboardBinding> {

    @Nullable
    @Override
    protected FragmentAdminDashboardBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentAdminDashboardBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
