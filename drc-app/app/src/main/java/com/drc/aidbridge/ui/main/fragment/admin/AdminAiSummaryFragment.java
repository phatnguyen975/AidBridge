package com.drc.aidbridge.ui.main.fragment.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminAiSummaryBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminAiSummaryFragment extends BaseFragment<FragmentAdminAiSummaryBinding> {

    @Nullable
    @Override
    protected FragmentAdminAiSummaryBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentAdminAiSummaryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        binding.textAdminAiSummaryTitle.setText(getString(R.string.admin_ai_screen_title));
    }

    @Override
    protected void observeViewModel() {
    }
}