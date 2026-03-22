package com.drc.aidbridge.ui.main.fragment.victim;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentVictimRescueTabBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimRescueTabFragment extends BaseFragment<FragmentVictimRescueTabBinding> {

    @Nullable
    @Override
    protected FragmentVictimRescueTabBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimRescueTabBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        
    }

    @Override
    protected void observeViewModel() {
        // TODO: Implement ViewModel observation for rescue data when backend integration is ready.
    }
}
