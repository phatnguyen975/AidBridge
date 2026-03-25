package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentSponsorHistoryBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorHistoryFragment extends BaseFragment<FragmentSponsorHistoryBinding> {

    @Nullable
    @Override
    protected FragmentSponsorHistoryBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorHistoryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
