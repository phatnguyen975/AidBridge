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
 *
 * Logic flow:
 * 1. Show the DRC logo with a fade-in animation.
 * 2. After SPLASH_DELAY_MS (2000ms), check if the user is already logged in:
 *    - If logged in (valid token exists): navigate to MainActivity (home map).
 *    - If NOT logged in: navigate to AuthActivity (guest/login/register flow).
 *
 * Uses ViewBinding for type-safe view access.
 *
 * NOTE: This Activity sets its own theme to Theme.AidBridge.Splash (fullscreen).
 */
@AndroidEntryPoint
public class SplashActivity extends BaseActivity<ActivitySplashBinding> {

    /** Injected by Hilt — used to check if a valid JWT token is stored. */
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
     *
     * Token presence is checked directly via TokenManager without an intermediate variable.
     *
     * TODO API INTEGRATION: Consider also validating JWT expiry locally (decode the
     * exp claim) so that an already-expired token routes the user to AuthActivity
     * rather than into MainActivity. TokenRefreshInterceptor handles silent refresh
     * during live API calls, but routing expired sessions here gives better UX.
     */
    private void navigateToNextScreen() {
        Intent intent;

        if (tokenManager.isLoggedIn()) {
            // Valid token present — user is authenticated
            intent = new Intent(this, MainActivity.class);
        } else {
            // No token — route to auth flow, starting at the Guest SOS screen
            intent = new Intent(this, AuthActivity.class);
        }
        startActivity(intent);
        finish(); // Prevent back-navigation to splash screen
    }
}
