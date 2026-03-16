package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentVolunteerMissionBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerMissionFragment extends BaseFragment<FragmentVolunteerMissionBinding> {

    @Nullable
    @Override
    protected FragmentVolunteerMissionBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVolunteerMissionBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
