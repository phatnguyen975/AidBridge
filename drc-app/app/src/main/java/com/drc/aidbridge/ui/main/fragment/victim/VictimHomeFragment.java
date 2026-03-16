package com.drc.aidbridge.ui.main.fragment.victim;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentVictimHomeBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimHomeFragment extends BaseFragment<FragmentVictimHomeBinding> {

    @Nullable
    @Override
    protected FragmentVictimHomeBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimHomeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
