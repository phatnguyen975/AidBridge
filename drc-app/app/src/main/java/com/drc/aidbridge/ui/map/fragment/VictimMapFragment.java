package com.drc.aidbridge.ui.map.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentMapVictimBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimMapFragment extends BaseFragment<FragmentMapVictimBinding> {

	@Nullable
	@Override
	protected FragmentMapVictimBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
		return FragmentMapVictimBinding.inflate(inflater, container, false);
	}

	@Override
	protected void setupViews() {
	}

	@Override
	protected void observeViewModel() {
	}
}
