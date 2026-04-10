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
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.ActivityMainBinding;
import com.drc.aidbridge.domain.enums.UserRole;
import com.drc.aidbridge.domain.usecase.auth.LogoutUseCase;
import com.drc.aidbridge.ui.auth.AuthActivity;
import com.drc.aidbridge.ui.base.BaseActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNavigation();
        
        // 1. Request Notification Permission for Android 13+
        askNotificationPermission();
        
        // 2. Fetch and Log FCM Token for Testing
        fetchFcmToken();
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

    private static final class RoleShellConfig {
        final int menuResId;
        final int navGraphResId;
        RoleShellConfig(int menuResId, int navGraphResId) {
            this.menuResId = menuResId;
            this.navGraphResId = navGraphResId;
        }
    }

    public void requestLogout() {
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
        Intent intent = new Intent(this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
