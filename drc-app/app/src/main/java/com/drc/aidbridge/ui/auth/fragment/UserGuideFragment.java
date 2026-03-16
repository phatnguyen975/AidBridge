package com.drc.aidbridge.ui.auth.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.drc.aidbridge.databinding.FragmentUserGuideBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * UserGuideFragment — the user guide screen accessible from the info icon.
 */
@AndroidEntryPoint
public class UserGuideFragment extends BaseFragment<FragmentUserGuideBinding> {

    @Override
    protected FragmentUserGuideBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentUserGuideBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        setupClickListeners();
    }

    @Override
    protected void observeViewModel() {
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> popBackStackSafely());
    }
}
