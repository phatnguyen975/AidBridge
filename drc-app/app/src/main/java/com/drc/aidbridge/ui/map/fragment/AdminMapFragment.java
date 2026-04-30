package com.drc.aidbridge.ui.map.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.ui.map.base.BaseMapFragment;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminMapViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminMapFragment extends BaseMapFragment<AdminMapViewModel> {

    private AdminMapViewModel adminMapViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adminMapViewModel = new ViewModelProvider(this).get(AdminMapViewModel.class);
    }

    @Override
    protected AdminMapViewModel getViewModel() {
        if (adminMapViewModel == null) {
            adminMapViewModel = new ViewModelProvider(this).get(AdminMapViewModel.class);
        }
        return adminMapViewModel;
    }

    @Override
    protected int getContentLayout() {
        return 0;
    }

    @Override
    protected void setupRoleSpecificUI() {
    }
}
