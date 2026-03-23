package com.drc.aidbridge.ui.map.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentGuestMapBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GuestMapFragment extends BaseFragment<FragmentGuestMapBinding> {

	@Nullable
	@Override
	protected FragmentGuestMapBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
		return FragmentGuestMapBinding.inflate(inflater, container, false);
	}

	@Override
	protected void setupViews() {
	}

	@Override
	protected void observeViewModel() {
	}
}
