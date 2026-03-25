package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentSponsorDashboardBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorDashboardFragment extends BaseFragment<FragmentSponsorDashboardBinding> {

    @Nullable
    @Override
    protected FragmentSponsorDashboardBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorDashboardBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
