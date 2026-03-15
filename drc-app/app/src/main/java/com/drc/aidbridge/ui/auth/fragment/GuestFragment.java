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
 * GuestFragment — the landing screen visible to unauthenticated users.
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
    }

    @Override
    protected void observeViewModel() {
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGpsStatus();
    }

    private void setupBottomNav() {
        binding.bottomNavGuest.setSelectedItemId(R.id.nav_guest_rescue);

        binding.bottomNavGuest.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_guest_rescue) {
                binding.layoutGuestRescue.setVisibility(View.VISIBLE);
                binding.layoutGuestMap.setVisibility(View.GONE);
                return true;
            } else if (id == R.id.nav_guest_map) {
                binding.layoutGuestRescue.setVisibility(View.GONE);
                binding.layoutGuestMap.setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        });
    }

    private void setupClickListeners() {
        // TODO: Wire up Quick SOS flow with location permission
        binding.btnSos.setOnClickListener(v -> {
            showToast("Tính năng SOS sẽ được tích hợp sau");
        });

        // Info icon → GuideFragment
        binding.ivInfo.setOnClickListener(v ->
            Navigation.findNavController(v)
                .navigate(R.id.action_guestFragment_to_guideFragment));

        // ĐĂNG NHẬP → LoginFragment
        binding.btnLogin.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_guestFragment_to_loginFragment));

        // ĐĂNG KÝ → RegisterFragment
        binding.tvRegisterLink.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_guestFragment_to_registerFragment));
    }

    private void updateGpsStatus() {
        if (getContext() == null) {
            return;
        }

        LocationManager lm = (LocationManager)
                requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        boolean gpsEnabled = lm != null &&
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (gpsEnabled) {
            binding.tvGpsStatus.setText(R.string.gps_active);
            binding.tvGpsStatus.setTextColor(getResources().getColor(R.color.safe_green, null));
        } else {
            binding.tvGpsStatus.setText(R.string.gps_inactive);
            binding.tvGpsStatus.setTextColor(getResources().getColor(R.color.sos_red, null));
        }
    }
}
