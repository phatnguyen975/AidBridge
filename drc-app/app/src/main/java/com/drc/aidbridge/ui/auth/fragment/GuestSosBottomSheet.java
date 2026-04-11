package com.drc.aidbridge.ui.auth.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.BottomSheetGuestSosBinding;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.auth.viewmodel.GuestSosViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GuestSosBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetGuestSosBinding binding;
    private GuestSosViewModel viewModel;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private Double currentLatitude;
    private Double currentLongitude;
    private GuestSosInput pendingInput;

    public static GuestSosBottomSheet newInstance() {
        return new GuestSosBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetGuestSosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(GuestSosViewModel.class);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupLocationPermissionLauncher();
        setupSeverityDefault();
        setupInteractions();
        observeViewModel();
        refreshLocationStatus(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupSeverityDefault() {
        String[] levels = getResources().getStringArray(R.array.sos_severity_levels);
        if (levels.length > 0) {
            binding.actSeverity.setText(levels[0], false);
        }
    }

    private void setupInteractions() {
        binding.btnGrantLocation.setOnClickListener(v -> refreshLocationStatus(true));
        binding.btnSubmitGuestSos.setOnClickListener(v -> submit());
    }

    private void observeViewModel() {
        viewModel.getValidationError().observe(getViewLifecycleOwner(), this::renderValidationError);
        viewModel.getSubmitResult().observe(getViewLifecycleOwner(), this::renderSubmitResult);
    }

    private void renderValidationError(@Nullable ValidationResult validation) {
        if (validation == null || validation.isValid() || binding == null) {
            return;
        }

        String message = validation.getErrorMessage();
        showError(message != null && !message.trim().isEmpty()
            ? message
            : getString(R.string.guest_sos_submit_error));
    }

    private void renderSubmitResult(@Nullable NetworkResultWrapper<String> result) {
        if (result == null || binding == null) {
            return;
        }

        if (result.hasBeenHandled()) {
            return;
        }

        if (result.isLoading()) {
            binding.btnSubmitGuestSos.setEnabled(false);
            binding.btnSubmitGuestSos.setText(R.string.guest_sos_submitting);
            return;
        }

        binding.btnSubmitGuestSos.setEnabled(true);
        binding.btnSubmitGuestSos.setText(R.string.guest_sos_submit);

        if (result.isSuccess()) {
            result.markAsHandled();
            String message = result.getData();
            showSuccess(message != null && !message.trim().isEmpty()
                ? message
                : getString(R.string.guest_sos_submit_success));
            dismissAllowingStateLoss();
            return;
        }

        if (result.isError()) {
            result.markAsHandled();
            String message = result.getMessage();
            showError(message != null && !message.trim().isEmpty()
                ? message
                : getString(R.string.guest_sos_submit_error));
        }
    }

    private void submit() {
        GuestSosInput input = collectInput();
        if (input == null) {
            return;
        }

        if (currentLatitude == null || currentLongitude == null) {
            pendingInput = input;
            refreshLocationStatus(true);
            return;
        }

        pendingInput = null;
        viewModel.submitSos(
            input.fullName,
            input.peopleCount,
            input.severity,
            input.healthNote,
            currentLatitude,
            currentLongitude
        );
    }

    @Nullable
    private GuestSosInput collectInput() {
        String fullName = getRawText(binding.etFullName).trim();
        String peopleCountText = getRawText(binding.etPeopleCount).trim();
        String severity = getRawText(binding.actSeverity).trim();
        String healthNote = getRawText(binding.etHealthDetail).trim();

        int peopleCount = 1;
        if (!peopleCountText.isEmpty()) {
            try {
                peopleCount = Integer.parseInt(peopleCountText);
            } catch (NumberFormatException ignored) {
                peopleCount = 1;
            }
        }

        if (peopleCount <= 0) {
            peopleCount = 1;
        }

        if (severity.isEmpty()) {
            String[] levels = getResources().getStringArray(R.array.sos_severity_levels);
            if (levels.length > 0) {
                severity = levels[0];
            }
        }

        return new GuestSosInput(fullName, peopleCount, severity, healthNote);
    }

    private void setupLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                if (isLocationPermissionGranted()) {
                    fetchCurrentLocation(pendingInput != null);
                    return;
                }

                showError(getString(R.string.guest_sos_location_permission_denied));
                updateLocationStatus(false);
                pendingInput = null;
            }
        );
    }

    private void refreshLocationStatus(boolean needLocationForSubmit) {
        if (!isAdded()) {
            return;
        }

        if (!isLocationPermissionGranted()) {
            if (needLocationForSubmit) {
                locationPermissionLauncher.launch(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                });
            } else {
                updateLocationStatus(false);
            }
            return;
        }

        fetchCurrentLocation(needLocationForSubmit);
    }

    private void fetchCurrentLocation(boolean trySubmitAfterFetch) {
        if (!isLocationProviderEnabled()) {
            updateLocationStatus(false);
            if (trySubmitAfterFetch) {
                pendingInput = null;
                showError(getString(R.string.guest_sos_location_disabled));
            }
            return;
        }

        try {
            fusedLocationProviderClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        updateLocationStatus(true);
                        submitIfPending();
                        return;
                    }

                    requestLastKnownLocation(trySubmitAfterFetch);
                })
                .addOnFailureListener(ignored -> requestLastKnownLocation(trySubmitAfterFetch));
        } catch (SecurityException ignored) {
            updateLocationStatus(false);
            if (trySubmitAfterFetch) {
                pendingInput = null;
                showError(getString(R.string.guest_sos_location_permission_denied));
            }
        }
    }

    private void requestLastKnownLocation(boolean trySubmitAfterFetch) {
        try {
            fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        updateLocationStatus(true);
                        submitIfPending();
                        return;
                    }

                    updateLocationStatus(false);
                    if (trySubmitAfterFetch) {
                        pendingInput = null;
                        showError(getString(R.string.guest_sos_location_unavailable));
                    }
                })
                .addOnFailureListener(ignored -> {
                    updateLocationStatus(false);
                    if (trySubmitAfterFetch) {
                        pendingInput = null;
                        showError(getString(R.string.guest_sos_location_unavailable));
                    }
                });
        } catch (SecurityException ignored) {
            updateLocationStatus(false);
            if (trySubmitAfterFetch) {
                pendingInput = null;
                showError(getString(R.string.guest_sos_location_permission_denied));
            }
        }
    }

    private void submitIfPending() {
        if (pendingInput == null || currentLatitude == null || currentLongitude == null) {
            return;
        }

        GuestSosInput input = pendingInput;
        pendingInput = null;
        viewModel.submitSos(
            input.fullName,
            input.peopleCount,
            input.severity,
            input.healthNote,
            currentLatitude,
            currentLongitude
        );
    }

    private void updateLocationStatus(boolean hasLocation) {
        if (binding == null) {
            return;
        }

        int textRes = hasLocation
            ? R.string.guest_sos_location_ready
            : R.string.guest_sos_location_missing;
        int colorRes = hasLocation ? R.color.safe_green : R.color.sos_red;

        binding.tvLocationStatus.setText(textRes);
        binding.tvLocationStatus.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
    }

    private boolean isLocationPermissionGranted() {
        Context context = requireContext();
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationProviderEnabled() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private String getRawText(EditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString();
    }

    private void showSuccess(String message) {
        if (binding == null) {
            return;
        }
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private void showError(String message) {
        if (binding == null) {
            return;
        }
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private static final class GuestSosInput {
        final String fullName;
        final int peopleCount;
        final String severity;
        final String healthNote;

        GuestSosInput(String fullName, int peopleCount, String severity, String healthNote) {
            this.fullName = fullName;
            this.peopleCount = peopleCount;
            this.severity = severity;
            this.healthNote = healthNote;
        }
    }
}
