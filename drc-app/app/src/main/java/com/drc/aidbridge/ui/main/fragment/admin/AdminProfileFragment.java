package com.drc.aidbridge.ui.main.fragment.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminProfileBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminProfileFragment extends BaseFragment<FragmentAdminProfileBinding> {

    @Nullable
    @Override
    protected FragmentAdminProfileBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentAdminProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        binding.textAdminProfileTitle.setText(getString(R.string.admin_profile_screen_title));
    }

    @Override
    protected void observeViewModel() {
    }
}