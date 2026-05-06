package com.drc.aidbridge.ui.map.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.ui.map.base.BaseMapFragment;
import com.drc.aidbridge.ui.main.viewmodel.sponsor.SponsorMapViewModel;

import dagger.hilt.android.AndroidEntryPoint;
import com.drc.aidbridge.R;
@AndroidEntryPoint
public class SponsorMapFragment extends BaseMapFragment<SponsorMapViewModel> {

    private SponsorMapViewModel sponsorMapViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sponsorMapViewModel = new ViewModelProvider(this).get(SponsorMapViewModel.class);
    }

    @Override
    protected SponsorMapViewModel getViewModel() {
        if (sponsorMapViewModel == null) {
            sponsorMapViewModel = new ViewModelProvider(this).get(SponsorMapViewModel.class);
        }
        return sponsorMapViewModel;
    }

    @Override
    protected int getContentLayout() {
        return R.layout.fragment_map_sponsor;
    }

    @Override
    protected void setupRoleSpecificUI() {
    }
}
