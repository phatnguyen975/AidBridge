package com.drc.aidbridge.ui.main.fragment.victim;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentVictimProfileBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimProfileFragment extends BaseFragment<FragmentVictimProfileBinding> {

    @Nullable
    @Override
    protected FragmentVictimProfileBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
