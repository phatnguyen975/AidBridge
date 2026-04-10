package com.drc.aidbridge.ui.auth;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ActivityAuthBinding;
import com.drc.aidbridge.ui.base.BaseActivity;
import com.google.firebase.messaging.FirebaseMessaging;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * AuthActivity — the host container for all authentication fragments.
 */
@AndroidEntryPoint
public class AuthActivity extends BaseActivity<ActivityAuthBinding> {

    private static final String TAG = "AuthActivity_FCM";
    public static final String EXTRA_DESTINATION = "destination";
    public static final String EXTRA_EMAIL = "email";
    public static final String DESTINATION_OTP_FRAGMENT = "otp_fragment";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted.");
                } else {
                    Log.w(TAG, "Notification permission denied.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        askNotificationPermission();
        fetchFcmToken();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void fetchFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                return;
            }
            String token = task.getResult();
            Log.d(TAG, "=============================================");
            Log.d(TAG, "FCM TOKEN: " + token);
            Log.d(TAG, "=============================================");
        });
    }

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
