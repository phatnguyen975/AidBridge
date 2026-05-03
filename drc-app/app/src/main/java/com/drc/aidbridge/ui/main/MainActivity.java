package com.drc.aidbridge.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.ActivityMainBinding;
import com.drc.aidbridge.domain.enums.UserRole;
import com.drc.aidbridge.domain.usecase.auth.LogoutUseCase;
import com.drc.aidbridge.service.EmergencyTrackingService;
import com.drc.aidbridge.service.UserLocationManager;
import com.drc.aidbridge.ui.auth.AuthActivity;
import com.drc.aidbridge.ui.base.BaseActivity;
import com.drc.aidbridge.ui.main.fragment.volunteer.VoluteerMissionAcceptanceFragment;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerTaskViewModel;
import com.drc.aidbridge.utils.Constants;
import com.drc.aidbridge.utils.TokenManager;
import com.google.firebase.messaging.FirebaseMessaging;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * MainActivity — the main authenticated shell, entered after a successful login.
 */
@AndroidEntryPoint
public class MainActivity extends BaseActivity<ActivityMainBinding> {

    private static final String TAG = "MainActivity_FCM";
    private static final long SOS_POPUP_ANIM_DURATION_MS = 180L;

    @Inject
    TokenManager tokenManager;

    @Inject
    LogoutUseCase logoutUseCase;

    @Inject
    UserLocationManager userLocationManager;

    private NavController navController;

    // Declare the launcher at the top of your Activity/Fragment:
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted.");
                } else {
                    Log.w(TAG, "Notification permission denied.");
                    Toast.makeText(this, "Bạn cần cấp quyền thông báo để nhận cập nhật cứu trợ.", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String[]> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (hasLocationPermission()) {
                    userLocationManager.startForegroundTracking();
                    return;
                }

                if (shouldTrackForegroundLocation()) {
                    Toast.makeText(
                        this,
                        R.string.main_location_permission_required,
                        Toast.LENGTH_LONG
                    ).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNavigation();

        askNotificationPermission();
        ensureForegroundLocationTracking();
        fetchFcmToken();
        handleLaunchIntent(getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (shouldTrackForegroundLocation() && hasLocationPermission()) {
            userLocationManager.startForegroundTracking();
        }
    }

    @Override
    protected void onStop() {
        userLocationManager.stopForegroundTracking();
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleLaunchIntent(intent);
    }

    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission already granted.");
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // Show an explanation to the user *asynchronously*
                Toast.makeText(this, "Ứng dụng cần quyền thông báo để gửi tin nhắn cứu trợ khẩn cấp.", Toast.LENGTH_LONG).show();
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void fetchFcmToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();

                // Log it for the developer to use in Firebase Console
                Log.d(TAG, "=============================================");
                Log.d(TAG, "FCM TOKEN: " + token);
                Log.d(TAG, "Use this token to test messages in Firebase Console.");
                Log.d(TAG, "=============================================");
            });
    }

    private void ensureForegroundLocationTracking() {
        if (!shouldTrackForegroundLocation()) {
            return;
        }

        if (hasLocationPermission()) {
            userLocationManager.startForegroundTracking();
            return;
        }

        requestLocationPermissionLauncher.launch(new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    @Override
    protected ActivityMainBinding inflateBinding(LayoutInflater inflater) {
        return ActivityMainBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void observeViewModel() {
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(binding.mainNavHost.getId());
        if (navHostFragment == null) {
            return;
        }

        navController = navHostFragment.getNavController();
        applyRoleShell();

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int destinationId = item.getItemId();

            if (destinationId == R.id.victimSosFragment) {
                showVictimSosPopupWindow();
                return false;
            }

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == destinationId) {
                return true;
            }

            try {
                navController.navigate(destinationId);
                return true;
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                return false;
            }
        });
        binding.bottomNav.setOnItemReselectedListener(item -> {
            if (item.getItemId() == R.id.victimSosFragment) {
                showVictimSosPopupWindow();
            }
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (binding == null) {
                return;
            }

            int destinationId = destination.getId();
            if (destinationId == R.id.volunteerSosAcceptanceFragment) {
                if (binding.bottomNav.getMenu().findItem(R.id.volunteerMissionListFragment) != null
                        && binding.bottomNav.getSelectedItemId() != R.id.volunteerMissionListFragment) {
                    binding.bottomNav.getMenu().findItem(R.id.volunteerMissionListFragment).setChecked(true);
                }
                return;
            }

            if (destinationId == R.id.victimSosSelfFragment
                    || destinationId == R.id.victimSosRelativeFragment) {
                if (binding.bottomNav.getSelectedItemId() != R.id.victimSosFragment) {
                    binding.bottomNav.getMenu().findItem(R.id.victimSosFragment).setChecked(true);
                }
                return;
            }

            if (binding.bottomNav.getMenu().findItem(destinationId) != null) {
                if (binding.bottomNav.getSelectedItemId() != destinationId) {
                    binding.bottomNav.getMenu().findItem(destinationId).setChecked(true);
                }
            }
        });

        binding.bottomNav.setSelectedItemId(navController.getGraph().getStartDestinationId());
    }

    private void handleLaunchIntent(Intent intent) {
        if (intent == null || navController == null) {
            return;
        }

        String notificationType = sanitize(intent.getStringExtra(Constants.EXTRA_NOTIFICATION_TYPE));
        if (!Constants.NOTIFICATION_TYPE_DISPATCH_REQUEST.equals(notificationType)) {
            return;
        }

        UserRole role = UserRole.fromStringSafe(tokenManager.getUserRole());
        if (role != UserRole.VOLUNTEER) {
            clearDispatchExtras(intent);
            return;
        }

        String missionId = sanitize(intent.getStringExtra(Constants.EXTRA_MISSION_ID));
        String dispatchAttemptId = sanitize(intent.getStringExtra(Constants.EXTRA_DISPATCH_ATTEMPT_ID));
        String missionType = sanitize(intent.getStringExtra(Constants.EXTRA_MISSION_TYPE));
        String expiresAt = sanitize(intent.getStringExtra(Constants.EXTRA_EXPIRES_AT));
        if (missionId == null || dispatchAttemptId == null) {
            clearDispatchExtras(intent);
            return;
        }

        new ViewModelProvider(this)
                .get(VolunteerTaskViewModel.class)
                .openDispatchRequest(missionId, dispatchAttemptId, missionType, expiresAt);

        if (binding.bottomNav.getMenu().findItem(R.id.volunteerMissionListFragment) != null) {
            binding.bottomNav.getMenu().findItem(R.id.volunteerMissionListFragment).setChecked(true);
        }

        if (navController.getGraph().findNode(R.id.volunteerSosAcceptanceFragment) != null
                && (navController.getCurrentDestination() == null
                || navController.getCurrentDestination().getId() != R.id.volunteerSosAcceptanceFragment)) {
            Bundle args = new Bundle();
            args.putString(VoluteerMissionAcceptanceFragment.ARG_MISSION_ID, missionId);
            args.putString(VoluteerMissionAcceptanceFragment.ARG_DISPATCH_ATTEMPT_ID, dispatchAttemptId);
            args.putString(VoluteerMissionAcceptanceFragment.ARG_MISSION_TYPE, missionType);
            args.putString(VoluteerMissionAcceptanceFragment.ARG_EXPIRES_AT, expiresAt);
            navController.navigate(
                    R.id.volunteerSosAcceptanceFragment,
                    args,
                    new NavOptions.Builder().setLaunchSingleTop(true).build()
            );
        }

        clearDispatchExtras(intent);
    }

    private void showVictimSosPopupWindow() {
        View anchor = binding.bottomNav.findViewById(R.id.victimSosFragment);
        if (anchor == null) {
            anchor = binding.bottomNav;
        }

        View popupView = LayoutInflater.from(this).inflate(R.layout.layout_victim_sos_popup, null, false);
        LinearLayout btnSosSelf = popupView.findViewById(R.id.btn_sos_self);
        LinearLayout btnSosRelative = popupView.findViewById(R.id.btn_sos_relative);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setElevation(getResources().getDimension(R.dimen.spacing_sm));
        }

        int currentDestId = navController.getCurrentDestination() != null
                ? navController.getCurrentDestination().getId()
                : -1;
        updateSosPopupButtonState(btnSosSelf, btnSosRelative, currentDestId);

        btnSosSelf.setOnClickListener(v -> dismissSosPopupWithAnimation(
            popupWindow,
            popupView,
            () -> navigateSafelyFromActivity(R.id.victimSosSelfFragment)
        ));

        btnSosRelative.setOnClickListener(v -> dismissSosPopupWithAnimation(
            popupWindow,
            popupView,
            () -> navigateSafelyFromActivity(R.id.victimSosRelativeFragment)
        ));

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = popupView.getMeasuredWidth();
        int popupHeight = popupView.getMeasuredHeight();
        int xOff = (anchor.getWidth() - popupWidth) / 2;
        int yOff = -(anchor.getHeight() + popupHeight + getResources().getDimensionPixelSize(R.dimen.spacing_sm));
        popupWindow.showAsDropDown(anchor, xOff, yOff);

        popupView.setAlpha(0f);
        popupView.setTranslationY(getResources().getDimension(R.dimen.spacing_sm));
        popupView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(SOS_POPUP_ANIM_DURATION_MS)
            .setInterpolator(new DecelerateInterpolator())
            .start();
    }

    private void updateSosPopupButtonState(@NonNull LinearLayout btnSosSelf,
                                           @NonNull LinearLayout btnSosRelative,
                                           int currentDestId) {
        btnSosSelf.setBackground(createSosPopupItemBackground(currentDestId == R.id.victimSosSelfFragment));
        btnSosRelative.setBackground(createSosPopupItemBackground(currentDestId == R.id.victimSosRelativeFragment));
    }

    @NonNull
    private GradientDrawable createSosPopupItemBackground(boolean isSelected) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(getResources().getDimension(R.dimen.radius_md));
        if (isSelected) {
            background.setColor(ContextCompat.getColor(this, R.color.bg_surface));
            background.setStroke(
                    getResources().getDimensionPixelSize(R.dimen.victim_sos_popup_divider_height),
                    ContextCompat.getColor(this, R.color.color_primary)
            );
        } else {
            background.setColor(Color.TRANSPARENT);
            background.setStroke(
                    getResources().getDimensionPixelSize(R.dimen.victim_sos_popup_divider_height),
                    Color.TRANSPARENT
            );
        }
        return background;
    }

    private void navigateSafelyFromActivity(int destinationId) {
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == destinationId) {
            return;
        }
        try {
            navController.navigate(destinationId);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
        }
    }

    private void dismissSosPopupWithAnimation(@NonNull PopupWindow popupWindow,
                                              @NonNull View popupView,
                                              @NonNull Runnable onDismissComplete) {
        popupView.animate()
                .alpha(0f)
                .translationY(getResources().getDimension(R.dimen.spacing_sm))
                .setDuration(SOS_POPUP_ANIM_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    popupWindow.dismiss();
                    onDismissComplete.run();
                })
                .start();
    }

    private void applyRoleShell() {
        String roleStr = tokenManager.getUserRole();
        UserRole role = UserRole.fromStringSafe(roleStr);
        RoleShellConfig config = resolveRoleShellConfig(role);

        binding.bottomNav.getMenu().clear();
        binding.bottomNav.inflateMenu(config.menuResId);
        navController.setGraph(config.navGraphResId);
    }

    @NonNull
    private RoleShellConfig resolveRoleShellConfig(@NonNull UserRole role) {
        switch (role) {
            case VICTIM:
                return new RoleShellConfig(R.menu.menu_bottom_nav_victim, R.navigation.nav_graph_victim);
            case VOLUNTEER:
                return new RoleShellConfig(R.menu.menu_bottom_nav_volunteer, R.navigation.nav_graph_volunteer);
            case SPONSOR:
                return new RoleShellConfig(R.menu.menu_bottom_nav_sponsor, R.navigation.nav_graph_sponsor);
            case STAFF:
                return new RoleShellConfig(R.menu.menu_bottom_nav_staff, R.navigation.nav_graph_staff);
            case ADMIN:
                return new RoleShellConfig(R.menu.menu_bottom_nav_admin, R.navigation.nav_graph_admin);
            case GUEST:
            default:
                return new RoleShellConfig(R.menu.menu_bottom_nav_victim, R.navigation.nav_graph_victim);
        }
    }

    private boolean shouldTrackForegroundLocation() {
        UserRole role = UserRole.fromStringSafe(tokenManager.getUserRole());
        return role == UserRole.VICTIM || role == UserRole.VOLUNTEER;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
    }

    private static final class RoleShellConfig {
        final int menuResId;
        final int navGraphResId;
        RoleShellConfig(int menuResId, int navGraphResId) {
            this.menuResId = menuResId;
            this.navGraphResId = navGraphResId;
        }
    }

    public void requestLogout() {
        EmergencyTrackingService.stopTracking(this);
        userLocationManager.stopForegroundTracking();
        LiveData<NetworkResultWrapper<Boolean>> logoutResult = logoutUseCase.execute();
        logoutResult.observe(this, result -> {
            if (result == null || result.isLoading() || result.hasBeenHandled()) {
                return;
            }

            result.markAsHandled();
            navigateToAuthShell();
        });
    }

    private void navigateToAuthShell() {
        EmergencyTrackingService.stopTracking(this);
        userLocationManager.stopForegroundTracking();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void clearDispatchExtras(@NonNull Intent intent) {
        intent.removeExtra(Constants.EXTRA_NOTIFICATION_TYPE);
        intent.removeExtra(Constants.EXTRA_NOTIFICATION_TITLE);
        intent.removeExtra(Constants.EXTRA_NOTIFICATION_BODY);
        intent.removeExtra(Constants.EXTRA_MISSION_ID);
        intent.removeExtra(Constants.EXTRA_DISPATCH_ATTEMPT_ID);
        intent.removeExtra(Constants.EXTRA_MISSION_TYPE);
        intent.removeExtra(Constants.EXTRA_DISPATCH_TYPE);
        intent.removeExtra(Constants.EXTRA_EXPIRES_AT);
        intent.removeExtra(Constants.EXTRA_CHANNEL_ID);
        intent.removeExtra(Constants.EXTRA_CLICK_ACTION);
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
