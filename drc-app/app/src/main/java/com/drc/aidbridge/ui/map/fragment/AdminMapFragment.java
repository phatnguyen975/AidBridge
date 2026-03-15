package com.drc.aidbridge.ui.map.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentMapAdminBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminMapFragment extends BaseFragment<FragmentMapAdminBinding> {

	@Nullable
	@Override
	protected FragmentMapAdminBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
		return FragmentMapAdminBinding.inflate(inflater, container, false);
	}

	@Override
	protected void setupViews() {
	}

	@Override
	protected void observeViewModel() {
	}
}
