package com.drc.aidbridge.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ActivityMainBinding;
import com.drc.aidbridge.domain.enums.UserRole;
import com.drc.aidbridge.ui.base.BaseActivity;
import com.drc.aidbridge.utils.TokenManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * MainActivity — the main authenticated shell, entered after a successful login.
 */
@AndroidEntryPoint
public class MainActivity extends BaseActivity<ActivityMainBinding> {

    @Inject
    TokenManager tokenManager;

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNavigation();
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
            try {
                return NavigationUI.onNavDestinationSelected(item, navController);
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                return false;
            }
        });
        binding.bottomNav.setOnItemReselectedListener(item -> {
            // Ignore reselect to avoid unnecessary duplicate navigation.
        });

        binding.bottomNav.setSelectedItemId(navController.getGraph().getStartDestinationId());
    }

    private void applyRoleShell() {
        // TODO: Use actual role after backend integration
        // String roleStr = tokenManager.getUserRole();
        // UserRole role = UserRole.fromStringSafe(roleStr);
        UserRole role = UserRole.VICTIM;
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
}
