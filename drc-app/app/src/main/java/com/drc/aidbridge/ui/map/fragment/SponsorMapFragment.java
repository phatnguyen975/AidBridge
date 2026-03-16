package com.drc.aidbridge.ui.map.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentMapSponsorBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorMapFragment extends BaseFragment<FragmentMapSponsorBinding> {

	@Nullable
	@Override
	protected FragmentMapSponsorBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
		return FragmentMapSponsorBinding.inflate(inflater, container, false);
	}

	@Override
	protected void setupViews() {
	}

	@Override
	protected void observeViewModel() {
	}
}
