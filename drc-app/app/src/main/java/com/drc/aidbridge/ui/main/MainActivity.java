package com.drc.aidbridge.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.drc.aidbridge.databinding.ActivityMainBinding;
import com.drc.aidbridge.ui.base.BaseActivity;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * MainActivity — the main authenticated shell, entered after a successful login.
 */
@AndroidEntryPoint
public class MainActivity extends BaseActivity<ActivityMainBinding> {

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
        NavigationUI.setupWithNavController(binding.bottomNav, navController);
    }
}
