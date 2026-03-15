package com.drc.aidbridge.ui.auth.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.navigation.Navigation;

import com.drc.aidbridge.databinding.FragmentGuideBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * GuideFragment — the user guide screen accessible from the info icon.
 */
@AndroidEntryPoint
public class GuideFragment extends BaseFragment<FragmentGuideBinding> {

    @Override
    protected FragmentGuideBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentGuideBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        setupClickListeners();
    }

    @Override
    protected void observeViewModel() {
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
    }
}
