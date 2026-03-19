package com.drc.aidbridge.ui.map.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentMapGuestBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GuestMapFragment extends BaseFragment<FragmentMapGuestBinding> {

	@Nullable
	@Override
	protected FragmentMapGuestBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
		return FragmentMapGuestBinding.inflate(inflater, container, false);
	}

	@Override
	protected void setupViews() {
	}

	@Override
	protected void observeViewModel() {
	}
}
