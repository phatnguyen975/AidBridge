package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.FragmentVolunteerDashboardBinding;
import com.drc.aidbridge.domain.model.VolunteerMission;
import com.drc.aidbridge.domain.model.volunteer.VolunteerDashboardInfo;
import com.drc.aidbridge.service.UserLocationManager;
import com.drc.aidbridge.service.VolunteerHeartbeatManager;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.MainActivity;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerDashboardViewModel;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerTaskViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerDashboardFragment extends BaseFragment<FragmentVolunteerDashboardBinding> {

    @Inject
    VolunteerHeartbeatManager volunteerHeartbeatManager;

    @Inject
    UserLocationManager userLocationManager;

    private VolunteerTaskViewModel volunteerTaskViewModel;
    private VolunteerDashboardViewModel volunteerDashboardViewModel;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private boolean isMissionAccepted;
    private boolean isMissionIgnored;
    private boolean isApplyingProfileState;
    private boolean currentOnlineStatus;

    @Nullable
    private Boolean pendingToggleStatus;

    @Nullable
    private Double currentLatitude;

    @Nullable
    private Double currentLongitude;

    @Override
    protected FragmentVolunteerDashboardBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentVolunteerDashboardBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        volunteerTaskViewModel = new ViewModelProvider(requireActivity()).get(VolunteerTaskViewModel.class);
        volunteerDashboardViewModel = new ViewModelProvider(requireActivity()).get(VolunteerDashboardViewModel.class);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupLocationPermissionLauncher();
        syncCurrentLocationFromCache();

        currentOnlineStatus = false;
        updateStatusUI(false);
        binding.switchOnlineStatus.setChecked(false);

        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userLocationManager.hasLocationPermission()) {
            userLocationManager.startForegroundTracking();
            syncCurrentLocationFromCache();
        }
        if (volunteerDashboardViewModel != null) {
            volunteerDashboardViewModel.loadProfileDashboard();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isAdded() || getActivity() == null) {
            return;
        }

        BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            MenuItem dashboardItem = bottomNav.getMenu().findItem(R.id.volunteerDashboardFragment);
            if (dashboardItem != null) {
                dashboardItem.setChecked(true);
            }
        }
    }

    @Override
    protected void observeViewModel() {
        if (volunteerTaskViewModel == null) {
            return;
        }

        if (volunteerDashboardViewModel != null) {
            volunteerDashboardViewModel.getVolunteerDashboardInfoResult().observe(
                getViewLifecycleOwner(),
                resultObserver(this::renderDashboardProfile, this::showNetworkError)
            );

            volunteerDashboardViewModel.getToggleStatusResult().observe(
                getViewLifecycleOwner(),
                this::handleToggleStatusState
            );
        }

        volunteerTaskViewModel.getIsMissionAccepted().observe(getViewLifecycleOwner(), isAccepted -> {
            isMissionAccepted = Boolean.TRUE.equals(isAccepted);
            renderMissionCardsState();
        });

        volunteerTaskViewModel.getIsMissionIgnored().observe(getViewLifecycleOwner(), isIgnored -> {
            isMissionIgnored = Boolean.TRUE.equals(isIgnored);
            renderMissionCardsState();
        });
    }

    private void renderDashboardProfile(@Nullable VolunteerDashboardInfo profileInfo) {
        if (profileInfo == null) {
            return;
        }

        String fullName = profileInfo.getFullName();
        if (fullName == null || fullName.trim().isEmpty()) {
            binding.tvUserName.setText(getString(R.string.volunteer_dashboard_user_name_placeholder));
        } else {
            binding.tvUserName.setText(fullName.trim());
        }

        Glide.with(this)
            .load(profileInfo.getAvatarUrl())
            .placeholder(R.drawable.ic_avatar)
            .error(R.drawable.ic_avatar)
            .circleCrop()
            .into(binding.ivAvatar);

        binding.tvCompletedCount.setText(String.valueOf(profileInfo.getTotalCompletedTasks()));
        currentOnlineStatus = profileInfo.isOnline();

        applyOnlineStatusToUi(currentOnlineStatus);
        syncHeartbeatWithOnlineState();
    }

    private void handleToggleStatusState(@Nullable NetworkResultWrapper<Boolean> result) {
        if (result == null || result.isLoading() || result.hasBeenHandled()) {
            return;
        }

        result.markAsHandled();
        if (result.isSuccess()) {
            handleToggleStatusSuccess(result.getData());
            return;
        }

        if (result.isError()) {
            handleToggleStatusError(result.getMessage());
        }
    }

    private void handleToggleStatusSuccess(@Nullable Boolean isOnline) {
        if (isOnline == null) {
            handleToggleStatusError(getString(R.string.error_generic));
            return;
        }

        currentOnlineStatus = isOnline;
        pendingToggleStatus = null;
        applyOnlineStatusToUi(currentOnlineStatus);
        syncHeartbeatWithOnlineState();

        showToast(getString(currentOnlineStatus
            ? R.string.volunteer_dashboard_toast_mode_ready
            : R.string.volunteer_dashboard_toast_mode_offline));
    }

    private void handleToggleStatusError(@Nullable String message) {
        pendingToggleStatus = null;
        applyOnlineStatusToUi(currentOnlineStatus);
        syncHeartbeatWithOnlineState();
        showNetworkError(message != null && !message.trim().isEmpty()
            ? message
            : getString(R.string.error_generic));
    }

    private void showNetworkError(String message) {
        showTopSnackbar(binding.getRoot(), message, true);
    }

    private void renderMissionCardsState() {
        boolean hasPending = volunteerTaskViewModel != null && volunteerTaskViewModel.hasPendingDispatch();
        boolean shouldShowEmergencyCard = hasPending && !isMissionAccepted && !isMissionIgnored;
        binding.cardEmergency.setVisibility(shouldShowEmergencyCard ? View.VISIBLE : View.GONE);
        binding.cardCurrentMission.setVisibility(isMissionAccepted ? View.VISIBLE : View.GONE);
    }

    private void setupClickListeners() {
        binding.switchOnlineStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isApplyingProfileState) {
                return;
            }

            pendingToggleStatus = isChecked;
            if (isChecked) {
                requestLocationAndEnableOnline();
                return;
            }

            volunteerHeartbeatManager.stop();
            volunteerDashboardViewModel.toggleStatus(false, null, null);
        });

        binding.cardUserInfo.setOnClickListener(
            v -> showToast(getString(R.string.volunteer_dashboard_toast_view_profile)));

        binding.cardCurrentMission.setOnClickListener(v -> {
            String missionType = volunteerTaskViewModel.getCurrentMissionType().getValue();
            if (VoluteerMissionAcceptanceFragment.isDeliveryMissionType(missionType)) {
                navigateToDestinationSafely(R.id.volunteerDeliveryMissionFragment);
                return;
            }
            navigateSafely(R.id.action_dashboard_to_current_sos_mission);
        });

        binding.cardCompleted.setOnClickListener(v -> navigateSafely(R.id.action_dashboard_to_history));

        binding.tvSeeAll.setOnClickListener(
            v -> showToast(getString(R.string.volunteer_dashboard_toast_see_all_notifications)));

        binding.btnDetails.setOnClickListener(v -> {
            VolunteerMission pendingMission = volunteerTaskViewModel.getPendingMission().getValue();
            if (pendingMission == null) {
                showToast(getString(R.string.volunteer_mission_acceptance_no_dispatch));
                return;
            }

            Bundle bundle = new Bundle();
            bundle.putString(VoluteerMissionAcceptanceFragment.ARG_MISSION_ID, pendingMission.getId());
            bundle.putString(VoluteerMissionAcceptanceFragment.ARG_DISPATCH_ATTEMPT_ID,
                pendingMission.getDispatchAttemptId());
            bundle.putString(VoluteerMissionAcceptanceFragment.ARG_MISSION_TYPE, pendingMission.getMissionType());
            bundle.putString(VoluteerMissionAcceptanceFragment.ARG_EXPIRES_AT,
                pendingMission.getExpiresAt() != null ? pendingMission.getExpiresAt().toString() : null);
            navigateSafely(R.id.action_dashboard_to_sos_acceptance, bundle);
        });

        binding.layoutAccountSecurity.setOnClickListener(
            v -> navigateSafely(R.id.action_dashboard_to_personal_info));

        binding.layoutLogout.setOnClickListener(v -> requestLogout());
    }

    private void setupLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                if (Boolean.TRUE.equals(pendingToggleStatus) && hasLocationPermission()) {
                    userLocationManager.startForegroundTracking();
                    syncCurrentLocationFromCache();
                    if (currentLatitude != null && currentLongitude != null) {
                        submitOnlineToggle(currentLatitude, currentLongitude);
                        return;
                    }
                    fetchCurrentLocationAndEnableOnline();
                    return;
                }

                if (Boolean.TRUE.equals(pendingToggleStatus)) {
                    pendingToggleStatus = null;
                    applyOnlineStatusToUi(currentOnlineStatus);
                    showTopSnackbar(
                        binding.getRoot(),
                        getString(R.string.volunteer_dashboard_location_permission_required),
                        true
                    );
                }
            }
        );
    }

    private void requestLocationAndEnableOnline() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }

        syncCurrentLocationFromCache();
        if (currentLatitude != null && currentLongitude != null) {
            submitOnlineToggle(currentLatitude, currentLongitude);
            return;
        }

        fetchCurrentLocationAndEnableOnline();
    }

    private void fetchCurrentLocationAndEnableOnline() {
        if (!Boolean.TRUE.equals(pendingToggleStatus)) {
            return;
        }

        if (!isLocationProviderEnabled()) {
            pendingToggleStatus = null;
            applyOnlineStatusToUi(currentOnlineStatus);
            showTopSnackbar(binding.getRoot(), getString(R.string.volunteer_dashboard_location_disabled), true);
            return;
        }

        try {
            fusedLocationProviderClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (!isAdded() || !Boolean.TRUE.equals(pendingToggleStatus)) {
                        return;
                    }

                    if (location != null) {
                        userLocationManager.updateLocation(location.getLatitude(), location.getLongitude());
                        submitOnlineToggle(location.getLatitude(), location.getLongitude());
                        return;
                    }

                    requestLastKnownLocationAndEnableOnline();
                })
                .addOnFailureListener(ignored -> requestLastKnownLocationAndEnableOnline());
        } catch (SecurityException ignored) {
            pendingToggleStatus = null;
            applyOnlineStatusToUi(currentOnlineStatus);
            showTopSnackbar(
                binding.getRoot(),
                getString(R.string.volunteer_dashboard_location_permission_required),
                true
            );
        }
    }

    private void requestLastKnownLocationAndEnableOnline() {
        try {
            fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (!isAdded() || !Boolean.TRUE.equals(pendingToggleStatus)) {
                        return;
                    }

                    if (location != null) {
                        userLocationManager.updateLocation(location.getLatitude(), location.getLongitude());
                        submitOnlineToggle(location.getLatitude(), location.getLongitude());
                        return;
                    }

                    pendingToggleStatus = null;
                    applyOnlineStatusToUi(currentOnlineStatus);
                    showTopSnackbar(
                        binding.getRoot(),
                        getString(R.string.volunteer_dashboard_location_unavailable),
                        true
                    );
                })
                .addOnFailureListener(ignored -> {
                    if (!isAdded() || !Boolean.TRUE.equals(pendingToggleStatus)) {
                        return;
                    }

                    pendingToggleStatus = null;
                    applyOnlineStatusToUi(currentOnlineStatus);
                    showTopSnackbar(
                        binding.getRoot(),
                        getString(R.string.volunteer_dashboard_location_unavailable),
                        true
                    );
                });
        } catch (SecurityException ignored) {
            pendingToggleStatus = null;
            applyOnlineStatusToUi(currentOnlineStatus);
            showTopSnackbar(
                binding.getRoot(),
                getString(R.string.volunteer_dashboard_location_permission_required),
                true
            );
        }
    }

    private void submitOnlineToggle(double latitude, double longitude) {
        currentLatitude = latitude;
        currentLongitude = longitude;
        userLocationManager.updateLocation(latitude, longitude);
        userLocationManager.startForegroundTracking();
        volunteerHeartbeatManager.updateLastKnownLocation(latitude, longitude);
        volunteerDashboardViewModel.toggleStatus(true, latitude, longitude);
    }

    private void syncHeartbeatWithOnlineState() {
        if (currentOnlineStatus && hasLocationPermission()) {
            userLocationManager.startForegroundTracking();
            volunteerHeartbeatManager.start(currentLatitude, currentLongitude);
            return;
        }

        volunteerHeartbeatManager.stop();
    }

    private void applyOnlineStatusToUi(boolean isOnline) {
        isApplyingProfileState = true;
        binding.switchOnlineStatus.setChecked(isOnline);
        updateStatusUI(isOnline);
        isApplyingProfileState = false;
    }

    private boolean hasLocationPermission() {
        Context context = getContext();
        if (context == null) {
            return false;
        }

        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationProviderEnabled() {
        LocationManager locationManager =
            (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void syncCurrentLocationFromCache() {
        if (!userLocationManager.hasLocationPermission()) {
            return;
        }

        UserLocationManager.LocationSnapshot snapshot = userLocationManager.getFreshLocation(
            UserLocationManager.DEFAULT_FRESH_LOCATION_MAX_AGE_MS
        );
        if (snapshot == null) {
            return;
        }

        currentLatitude = snapshot.getLatitude();
        currentLongitude = snapshot.getLongitude();
    }

    private void updateStatusUI(boolean isOnline) {
        if (isOnline) {
            binding.tvStatusDescription
                .setText(R.string.volunteer_dashboard_status_online_placeholder);
            binding.viewStatusIndicator.setBackgroundResource(R.drawable.bg_circle_status_online);
            binding.btnReady.setAlpha(1.0f);
            binding.btnOffline.setAlpha(0.5f);
        } else {
            binding.tvStatusDescription
                .setText(R.string.volunteer_dashboard_status_offline_placeholder);
            binding.viewStatusIndicator.setBackgroundResource(R.drawable.bg_circle_status_offline);
            binding.btnReady.setAlpha(0.5f);
            binding.btnOffline.setAlpha(1.0f);
        }
    }

    private void requestLogout() {
        volunteerHeartbeatManager.stop();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).requestLogout();
        }
    }
}
