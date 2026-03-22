package com.drc.aidbridge.ui.main.fragment.victim;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentVictimSupplyTabBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimSupplyTabFragment extends BaseFragment<FragmentVictimSupplyTabBinding> {

    @Nullable
    @Override
    protected FragmentVictimSupplyTabBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimSupplyTabBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        
    }

    @Override
    protected void observeViewModel() {
        // TODO: Implement ViewModel observation for supply data when backend integration is ready.
    }
}
