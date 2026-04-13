package com.drc.aidbridge.ui.auth.fragment;

import android.location.LocationManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.navigation.NavController;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentGuestRescueBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * GuestRescueFragment — guest rescue entry screen.
 */
@AndroidEntryPoint
public class GuestRescueFragment extends BaseFragment<FragmentGuestRescueBinding> {

    @Override
    protected FragmentGuestRescueBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentGuestRescueBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
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

    private void setupClickListeners() {
        binding.btnSos.setOnClickListener(v -> {
            GuestSosBottomSheet.newInstance().show(getChildFragmentManager(), "guest_sos_sheet");
        });

        // Use the auth host controller (parent graph), not the nested guest-tabs controller.
        NavController parentNavController = getHostNavController(R.id.auth_nav_host);
        if (parentNavController == null) {
            return;
        }

        // Info icon → UserGuideFragment
        binding.ivInfo.setOnClickListener(v ->
                navigateSafely(parentNavController, R.id.action_guestShellFragment_to_userGuideFragment));

        // ĐĂNG NHẬP → LoginFragment
        binding.btnLogin.setOnClickListener(v ->
                navigateSafely(parentNavController, R.id.action_guestShellFragment_to_loginFragment));

        // ĐĂNG KÝ → RegisterFragment
        binding.tvRegisterLink.setOnClickListener(v ->
                navigateSafely(parentNavController, R.id.action_guestShellFragment_to_registerFragment));
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
