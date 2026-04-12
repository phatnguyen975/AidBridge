package com.drc.aidbridge.ui.map.victim;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentVictimMapBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimMapFragment extends BaseFragment<FragmentVictimMapBinding> {

    @Nullable
    @Override
    protected FragmentVictimMapBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimMapBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
