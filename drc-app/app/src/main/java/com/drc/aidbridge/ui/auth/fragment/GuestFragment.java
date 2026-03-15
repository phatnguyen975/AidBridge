package com.drc.aidbridge.ui.auth.fragment;

import android.location.LocationManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.navigation.Navigation;
import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentGuestBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * GuestFragment — the landing/emergency screen visible to unauthenticated users.
 *
 * Has a BottomNavigationView with 2 tabs:
 *   - ‘nav_rescue’   : SOS content (default)
 *   - ‘nav_map_view’ : Map placeholder (Phase 3)
 *
 * Key actions:
 *   - SOS button      → TODO Phase 3: Quick SOS flow
 *   - Info icon       → GuideFragment
 *   - ĐĂNG NHẬP      → LoginFragment
 *   - Đăng ký link   → RegisterFragment
 */
@AndroidEntryPoint
public class GuestFragment extends BaseFragment<FragmentGuestBinding> {

    @Override
    protected FragmentGuestBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentGuestBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        setupBottomNav();
        setupClickListeners();
        updateGpsStatus();
    }

    @Override
    protected void observeViewModel() {
    }

    // ------------------------------------------------------------------
    // Bottom navigation
    // ------------------------------------------------------------------

    /**
     * Handles Guest screen BottomNavigation (2 tabs: Cứu hộ + Bản đồ).
     * Swaps visibility between layout_rescue and layout_map_view.
     */
    private void setupBottomNav() {
        // Default: Cứu hộ tab selected
        binding.bottomNavGuest.setSelectedItemId(R.id.nav_rescue);

        binding.bottomNavGuest.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_rescue) {
                binding.layoutRescue.setVisibility(View.VISIBLE);
                binding.layoutMapView.setVisibility(View.GONE);
                return true;
            } else if (id == R.id.nav_map_view) {
                binding.layoutRescue.setVisibility(View.GONE);
                binding.layoutMapView.setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------
    // Click listeners
    // ------------------------------------------------------------------

    private void setupClickListeners() {
        // SOS button — TODO PHASE 3: Wire up Quick SOS flow with location permission
        binding.btnSos.setOnClickListener(v -> {
            showToast("Tính năng SOS Khẩn cấp sẽ được tích hợp ở Phase 3");
        });

        // Info icon → GuideFragment
        binding.ivInfo.setOnClickListener(v ->
            Navigation.findNavController(v)
                .navigate(R.id.action_guestFragment_to_guideFragment));

        // ĐĂNG NHẬP → LoginFragment
        binding.btnLogin.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_guestFragment_to_loginFragment));

        // Đăng ký tài khoản mới → RegisterFragment
        binding.tvRegisterLink.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_guestFragment_to_registerFragment));
    }

    // ------------------------------------------------------------------
    // GPS status
    // ------------------------------------------------------------------

    /**
     * Checks current GPS availability and updates the status badge text.
     */
    private void updateGpsStatus() {
        if (getContext() == null) return;

        LocationManager lm = (LocationManager)
                requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        boolean gpsEnabled = lm != null &&
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (gpsEnabled) {
            binding.tvGpsStatus.setText(R.string.gps_active);
        } else {
            binding.tvGpsStatus.setText(R.string.gps_inactive);
        }
    }
}
