package com.drc.aidbridge.ui.auth;

import android.view.LayoutInflater;

import com.drc.aidbridge.databinding.ActivityAuthBinding;
import com.drc.aidbridge.ui.base.BaseActivity;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * AuthActivity — the host container for all authentication fragments.
 */
@AndroidEntryPoint
public class AuthActivity extends BaseActivity<ActivityAuthBinding> {

    @Override
    protected ActivityAuthBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAuthBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }
}
