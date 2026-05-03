package com.drc.aidbridge.ui.map.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.ui.map.base.BaseMapFragment;
import com.drc.aidbridge.ui.main.viewmodel.staff.StaffMapViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffMapFragment extends BaseMapFragment<StaffMapViewModel> {

    private StaffMapViewModel staffMapViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        staffMapViewModel = new ViewModelProvider(this).get(StaffMapViewModel.class);
    }

    @Override
    protected StaffMapViewModel getViewModel() {
        if (staffMapViewModel == null) {
            staffMapViewModel = new ViewModelProvider(this).get(StaffMapViewModel.class);
        }
        return staffMapViewModel;
    }

    @Override
    protected int getContentLayout() {
        return 0;
    }

    @Override
    protected void setupRoleSpecificUI() {
    }
}
