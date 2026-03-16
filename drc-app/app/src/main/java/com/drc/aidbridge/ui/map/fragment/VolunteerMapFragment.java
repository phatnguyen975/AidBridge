package com.drc.aidbridge.ui.map.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentMapVolunteerBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerMapFragment extends BaseFragment<FragmentMapVolunteerBinding> {

	@Nullable
	@Override
	protected FragmentMapVolunteerBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
		return FragmentMapVolunteerBinding.inflate(inflater, container, false);
	}

	@Override
	protected void setupViews() {
	}

	@Override
	protected void observeViewModel() {
	}
}
