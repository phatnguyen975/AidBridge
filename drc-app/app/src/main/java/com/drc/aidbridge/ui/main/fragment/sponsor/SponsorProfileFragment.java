package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentSponsorProfileBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorProfileFragment extends BaseFragment<FragmentSponsorProfileBinding> {

    @Nullable
    @Override
    protected FragmentSponsorProfileBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
