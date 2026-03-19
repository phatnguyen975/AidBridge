package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentSponsorHomeBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorHomeFragment extends BaseFragment<FragmentSponsorHomeBinding> {

    @Nullable
    @Override
    protected FragmentSponsorHomeBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorHomeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
