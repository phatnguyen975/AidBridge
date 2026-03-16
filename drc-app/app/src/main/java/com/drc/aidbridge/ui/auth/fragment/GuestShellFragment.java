package com.drc.aidbridge.ui.auth.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.drc.aidbridge.databinding.FragmentGuestShellBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GuestShellFragment extends BaseFragment<FragmentGuestShellBinding> {

    @Override
    protected FragmentGuestShellBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentGuestShellBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        setupTabsNavigation();
    }

    @Override
    protected void observeViewModel() {
    }

    private void setupTabsNavigation() {
        NavHostFragment tabsNavHost = (NavHostFragment) getChildFragmentManager()
                .findFragmentById(binding.guestTabsNavHost.getId());

        if (tabsNavHost == null) {
            return;
        }

        NavController tabsNavController = tabsNavHost.getNavController();

        binding.bottomNavGuest.setOnItemSelectedListener(item -> {
            try {
                return NavigationUI.onNavDestinationSelected(item, tabsNavController);
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                return false;
            }
        });
        binding.bottomNavGuest.setOnItemReselectedListener(item -> {
            // Ignore reselect to avoid unnecessary duplicate navigation.
        });

        binding.bottomNavGuest.setSelectedItemId(tabsNavController.getGraph().getStartDestinationId());
    }
}
