package com.drc.aidbridge.ui.map.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.ui.map.base.BaseMapFragment;
import com.drc.aidbridge.ui.main.viewmodel.guest.GuestMapViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GuestMapFragment extends BaseMapFragment<GuestMapViewModel> {

    private GuestMapViewModel guestMapViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        guestMapViewModel = new ViewModelProvider(this).get(GuestMapViewModel.class);
    }

    @Override
    protected GuestMapViewModel getViewModel() {
        if (guestMapViewModel == null) {
            guestMapViewModel = new ViewModelProvider(this).get(GuestMapViewModel.class);
        }
        return guestMapViewModel;
    }

    @Override
    protected int getContentLayout() {
        return 0;
    }

    @Override
    protected void setupRoleSpecificUI() {
    }
}
