package com.drc.aidbridge.ui.map.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentMapStaffBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffMapFragment extends BaseFragment<FragmentMapStaffBinding> {

	@Nullable
	@Override
	protected FragmentMapStaffBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
		return FragmentMapStaffBinding.inflate(inflater, container, false);
	}

	@Override
	protected void setupViews() {
	}

	@Override
	protected void observeViewModel() {
	}
}
