package com.drc.aidbridge.ui.map.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.ui.map.base.BaseMapFragment;
import com.drc.aidbridge.ui.main.viewmodel.victim.VictimMapViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimMapFragment extends BaseMapFragment<VictimMapViewModel> {

    private VictimMapViewModel victimMapViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        victimMapViewModel = new ViewModelProvider(this).get(VictimMapViewModel.class);
    }

    @Override
    protected VictimMapViewModel getViewModel() {
        if (victimMapViewModel == null) {
            victimMapViewModel = new ViewModelProvider(this).get(VictimMapViewModel.class);
        }
        return victimMapViewModel;
    }

    @Override
    protected int getContentLayout() {
        return 0;
    }

    @Override
    protected void setupRoleSpecificUI() {
    }
}
