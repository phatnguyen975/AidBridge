package com.drc.aidbridge.ui.main.fragment.victim;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentVictimSosRelativeBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimSosRelativeFragment extends BaseFragment<FragmentVictimSosRelativeBinding> {

    @Nullable
    @Override
    protected FragmentVictimSosRelativeBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimSosRelativeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        
    }

    @Override
    protected void observeViewModel() {
        // TODO: Observe relative SOS flow when feature is implemented.
    }
}
