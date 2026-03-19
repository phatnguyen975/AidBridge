package com.drc.aidbridge.ui.splash;

import android.content.Intent;
import android.view.animation.AnimationUtils;
import android.view.LayoutInflater;

import com.drc.aidbridge.databinding.ActivitySplashBinding;
import com.drc.aidbridge.ui.auth.AuthActivity;
import com.drc.aidbridge.ui.base.BaseActivity;
import com.drc.aidbridge.ui.main.MainActivity;
import com.drc.aidbridge.utils.Constants;
import com.drc.aidbridge.utils.TokenManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * SplashActivity — the launch screen shown for 2 seconds while the app initializes.
 */
@AndroidEntryPoint
public class SplashActivity extends BaseActivity<ActivitySplashBinding> {

    @Inject
    TokenManager tokenManager;

    @Override
    protected ActivitySplashBinding inflateBinding(LayoutInflater inflater) {
        return ActivitySplashBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        playFadeInAnimation();
        navigateAfterDelay();
    }

    @Override
    protected void observeViewModel() {
    }

    /**
     * Plays a fade-in animation on all visible elements (logo, app name, tagline).
     * Each element animates slightly offset for a staggered appearance.
     */
    private void playFadeInAnimation() {
        // Logo fades in first
        binding.ivLogo.animate()
                .alpha(1f)
                .setDuration(700)
                .setStartDelay(200)
                .start();
        // App name fades in shortly after
        binding.tvAppName.animate()
                .alpha(1f)
                .setDuration(700)
                .setStartDelay(500)
                .start();
        // Tagline fades in last
        binding.tvTagline.animate()
                .alpha(1f)
                .setDuration(700)
                .setStartDelay(700)
                .start();
    }

    /**
     * Schedules navigation to the next screen after the splash delay.
     * Uses a simple Handler.postDelayed for the 2-second wait.
     */
    private void navigateAfterDelay() {
        binding.getRoot().postDelayed(this::navigateToNextScreen, Constants.SPLASH_DELAY_MS);
    }

    /**
     * Determines the next screen based on the current authentication state:
     * - Token present and non-empty → navigate to MainActivity (authenticated shell).
     * - No token                    → navigate to AuthActivity (Guest → Login/Register).
     */
    private void navigateToNextScreen() {
        Intent intent;

        if (tokenManager.hasActiveSession()) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, AuthActivity.class);
        }

        startActivity(intent);
        finish(); // Prevent back-navigation to splash screen
    }
}
