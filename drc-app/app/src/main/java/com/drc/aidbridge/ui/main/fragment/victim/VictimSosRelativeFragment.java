package com.drc.aidbridge.ui.main.fragment.victim;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVictimSosRelativeBinding;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.victim.VictimSosViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimSosRelativeFragment extends BaseFragment<FragmentVictimSosRelativeBinding> {

    private VictimSosViewModel viewModel;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private RelativeSosFormInput pendingSubmitInput;
    private Double currentLatitude;
    private Double currentLongitude;

    @Nullable
    @Override
    protected FragmentVictimSosRelativeBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimSosRelativeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(VictimSosViewModel.class);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        setupLocationPermissionLauncher();

        String[] severityLevels = getResources().getStringArray(R.array.sos_severity_levels);
        if (severityLevels.length > 0) {
            binding.actRelativeSeverity.setText(severityLevels[0], false);
        }

        binding.btnSubmitRelativeSos.setOnClickListener(v -> extractDataAndSubmit());
        fetchCurrentLocation(false);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getValidationError().observe(getViewLifecycleOwner(), this::renderValidationError);

        viewModel.getSubmitRelativeSosResult().observe(
            getViewLifecycleOwner(),
            resultObserver(this::handleSubmitSuccess, this::handleSubmitError)
        );
    }

    private void renderValidationError(@Nullable ValidationResult validation) {
        if (validation == null || validation.isValid()) {
            return;
        }

        String message = validation.getErrorMessage();
        showTopSnackbar(
            binding.getRoot(),
            message != null && !message.trim().isEmpty()
                ? message
                : getString(R.string.victim_relative_sos_submit_error),
            true
        );
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        binding.btnSubmitRelativeSos.setEnabled(!isLoading);
        binding.btnSubmitRelativeSos.setAlpha(isLoading ? 0.7f : 1f);
    }

    private void extractDataAndSubmit() {
        clearInputFocusAndHideKeyboard();
        RelativeSosFormInput rawInput = collectRawInput();
        if (rawInput == null) {
            return;
        }

        if (currentLatitude == null || currentLongitude == null) {
            pendingSubmitInput = rawInput;
            fetchCurrentLocation(true);
            return;
        }

        submitRelativeSos(rawInput, currentLatitude, currentLongitude);
    }

    @Nullable
    private RelativeSosFormInput collectRawInput() {
        String name = getRawText(binding.etRelativeName).trim();
        String address = getRawText(binding.etRelativeAddress).trim();
        String phone = getRawText(binding.etRelativePhone).trim();
        String severity = getRawText(binding.actRelativeSeverity).trim();

        if (severity.isEmpty()) {
            severity = getDefaultSeverity();
        }

        return new RelativeSosFormInput(name, address, phone, severity);
    }

    private void handleSubmitSuccess(@Nullable String message) {
        pendingSubmitInput = null;
        String successMessage = (message != null && !message.trim().isEmpty())
            ? message
            : getString(R.string.victim_relative_sos_submit_success);
        showTopSnackbar(binding.getRoot(), successMessage, false);
        clearFormAfterSubmit();
    }

    private void handleSubmitError(String message) {
        pendingSubmitInput = null;
        String errorMessage = (message != null && !message.trim().isEmpty())
            ? message
            : getString(R.string.victim_relative_sos_submit_error);
        showTopSnackbar(binding.getRoot(), errorMessage, true);
    }

    private void clearFormAfterSubmit() {
        binding.etRelativeName.setText(null);
        binding.etRelativeAddress.setText(null);
        binding.etRelativePhone.setText(null);

        String defaultSeverity = getDefaultSeverity();
        if (!defaultSeverity.isEmpty()) {
            binding.actRelativeSeverity.setText(defaultSeverity, false);
        }
    }

    private void setupLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                if (isLocationPermissionGranted()) {
                    fetchCurrentLocation(pendingSubmitInput != null);
                    return;
                }

                showTopSnackbar(
                    binding.getRoot(),
                    getString(R.string.victim_relative_sos_permission_location_denied),
                    true
                );
                pendingSubmitInput = null;
            }
        );
    }

    private void fetchCurrentLocation(boolean trySubmitAfterFetch) {
        if (!isAdded()) {
            return;
        }

        if (!isLocationPermissionGranted()) {
            locationPermissionLauncher.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }

        if (!isLocationProviderEnabled()) {
            if (trySubmitAfterFetch) {
                pendingSubmitInput = null;
                showTopSnackbar(binding.getRoot(), getString(R.string.victim_relative_sos_location_unavailable), true);
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
                        submitIfPending();
                        return;
                    }

                    requestLastKnownLocation(trySubmitAfterFetch);
                })
                .addOnFailureListener(ignored -> requestLastKnownLocation(trySubmitAfterFetch));
        } catch (SecurityException ignored) {
            showTopSnackbar(
                binding.getRoot(),
                getString(R.string.victim_relative_sos_permission_location_denied),
                true
            );
            pendingSubmitInput = null;
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
                        submitIfPending();
                        return;
                    }

                    if (trySubmitAfterFetch) {
                        pendingSubmitInput = null;
                        showTopSnackbar(
                            binding.getRoot(),
                            getString(R.string.victim_relative_sos_location_unavailable),
                            true
                        );
                    }
                })
                .addOnFailureListener(ignored -> {
                    if (trySubmitAfterFetch) {
                        pendingSubmitInput = null;
                        showTopSnackbar(
                            binding.getRoot(),
                            getString(R.string.victim_relative_sos_location_unavailable),
                            true
                        );
                    }
                });
        } catch (SecurityException ignored) {
            if (trySubmitAfterFetch) {
                pendingSubmitInput = null;
                showTopSnackbar(
                    binding.getRoot(),
                    getString(R.string.victim_relative_sos_permission_location_denied),
                    true
                );
            }
        }
    }

    private void submitIfPending() {
        if (pendingSubmitInput == null || currentLatitude == null || currentLongitude == null) {
            return;
        }

        RelativeSosFormInput input = pendingSubmitInput;
        pendingSubmitInput = null;
        submitRelativeSos(input, currentLatitude, currentLongitude);
    }

    private void submitRelativeSos(RelativeSosFormInput input, double latitude, double longitude) {
        viewModel.submitRelativeSos(
            input.name,
            input.phone,
            input.address,
            input.severity,
            latitude,
            longitude
        );
    }

    private boolean isLocationPermissionGranted() {
        Context context = requireContext();
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
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

    private String getDefaultSeverity() {
        String[] levels = getResources().getStringArray(R.array.sos_severity_levels);
        if (levels.length == 0) {
            return "";
        }
        return levels[0];
    }

    private static final class RelativeSosFormInput {
        final String name;
        final String address;
        final String phone;
        final String severity;

        RelativeSosFormInput(String name, String address, String phone, String severity) {
            this.name = name;
            this.address = address;
            this.phone = phone;
            this.severity = severity;
        }
    }
}
