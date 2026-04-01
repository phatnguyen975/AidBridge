package com.drc.aidbridge.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ActivityAuthBinding;
import com.drc.aidbridge.ui.base.BaseActivity;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * AuthActivity — the host container for all authentication fragments.
 */
@AndroidEntryPoint
public class AuthActivity extends BaseActivity<ActivityAuthBinding> {

    public static final String EXTRA_DESTINATION = "destination";
    public static final String EXTRA_EMAIL = "email";
    public static final String DESTINATION_OTP_FRAGMENT = "otp_fragment";

    @Override
    protected ActivityAuthBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAuthBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        routeToRequestedDestination();
    }

    @Override
    protected void observeViewModel() {
    }

    private void routeToRequestedDestination() {
        String destination = getIntent() != null ? getIntent().getStringExtra(EXTRA_DESTINATION) : null;
        if (!DESTINATION_OTP_FRAGMENT.equals(destination)) {
            return;
        }

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
            .findFragmentById(R.id.auth_nav_host);
        if (navHostFragment == null) {
            return;
        }

        NavController navController = navHostFragment.getNavController();
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.otpFragment) {
            return;
        }

        Bundle args = new Bundle();
        String email = getIntent().getStringExtra(EXTRA_EMAIL);
        args.putString("email", email != null ? email : "");
        navController.navigate(R.id.otpFragment, args);
    }
}
