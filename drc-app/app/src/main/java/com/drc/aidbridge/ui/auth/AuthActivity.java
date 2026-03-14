package com.drc.aidbridge.ui.auth;

import android.view.LayoutInflater;
import com.drc.aidbridge.databinding.ActivityAuthBinding;
import com.drc.aidbridge.ui.base.BaseActivity;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * AuthActivity — the host container for all authentication fragments.
 *
 * Manages the Navigation Component back-stack for:
 * GuestFragment → LoginFragment | RegisterFragment → OtpFragment
 *
 * After successful authentication, activities in this stack navigate to
 * MainActivity and this Activity finishes (preventing back-nav to login).
 *
 * The actual content is rendered entirely by fragments — AuthActivity is
 * intentionally thin (just a NavHostFragment container).
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
