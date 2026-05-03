package com.drc.aidbridge.ui.main.fragment.victim;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.BuildConfig;
import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVictimRescueTabBinding;
import com.drc.aidbridge.domain.model.QuickSosSubmissionResult;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.service.EmergencyTrackingService;
import com.drc.aidbridge.service.UserLocationManager;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.victim.VictimImageAdapter;
import com.drc.aidbridge.ui.main.viewmodel.victim.VictimSosViewModel;
import com.drc.aidbridge.utils.NetworkMonitor;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimRescueTabFragment extends BaseFragment<FragmentVictimRescueTabBinding> {

    private static final int MAX_SCENE_IMAGES = 5;
    private static final String SMS_TAG = "AidBridgeSmsFallback";

    @Inject
    UserLocationManager userLocationManager;

    @Inject
    NetworkMonitor networkMonitor;

    private VictimSosViewModel viewModel;
    private VictimImageAdapter imageAdapter;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private final List<Uri> selectedImageUris = new ArrayList<>();

    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;
    private ActivityResultLauncher<String> sendSmsPermissionLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImagesLauncher;

    private Uri pendingCameraImageUri;
    private boolean pendingQuickSos;
    private boolean pendingQuickSosAfterSmsPermission;

    private Double currentLatitude;
    private Double currentLongitude;
    private Double currentAccuracy;
    private Long currentCapturedAtMillis;

    @Nullable
    @Override
    protected FragmentVictimRescueTabBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimRescueTabBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(VictimSosViewModel.class);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupActivityResultLaunchers();
        setupImageRecyclerView();
        setupSeverityDefault();
        setupInteractions();
        syncCurrentLocationFromCache();
        userLocationManager.refreshOnce();
        if (userLocationManager.hasLocationPermission()) {
            fetchCurrentLocation(false);
        }
        viewModel.loadCachedUser();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getCachedUserResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null || result.isLoading() || result.hasBeenHandled()) {
                return;
            }

            result.markAsHandled();
            if (result.isSuccess()) {
                bindCachedUser(result.getData());
            }
        });

        viewModel.getValidationError().observe(getViewLifecycleOwner(), this::renderValidationError);

        viewModel.getSubmitQuickSosResult().observe(
            getViewLifecycleOwner(),
            resultObserver(this::handleSubmitSuccess, this::handleSubmitError)
        );
    }

    private void bindCachedUser(@Nullable User user) {
        if (user == null || user.getName() == null || user.getName().trim().isEmpty()) {
            return;
        }

        binding.etFullName.setText(user.getName().trim());
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
                : getString(R.string.victim_rescue_submit_error),
            true
        );
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        binding.btnSos.setEnabled(!isLoading);
        binding.btnSos.setAlpha(isLoading ? 0.7f : 1f);
        binding.cvUploadArea.setEnabled(!isLoading);
        binding.tvSosHint.setText(isLoading
            ? R.string.victim_rescue_loading_hint
            : R.string.victim_rescue_sos_hint
        );
    }

    private void setupImageRecyclerView() {
        imageAdapter = new VictimImageAdapter(this::onImageRemoved);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.rvImages.setLayoutManager(layoutManager);
        binding.rvImages.setAdapter(imageAdapter);
    }

    private void setupSeverityDefault() {
        String[] levels = getResources().getStringArray(R.array.sos_severity_levels);
        if (levels.length > 0) {
            binding.actSeverity.setText(levels[0], false);
        }
    }

    private void setupInteractions() {
        binding.btnSos.setOnClickListener(v -> {
            clearInputFocusAndHideKeyboard();
            submitQuickSos();
        });

        binding.cvUploadArea.setOnClickListener(v -> showImageSourceDialog());
    }

    private void submitQuickSos() {
        syncCurrentLocationFromCache();
        if (currentLatitude == null || currentLongitude == null) {
            pendingQuickSos = true;
            fetchCurrentLocation(true);
            return;
        }

        if (shouldRequestSendSmsPermissionBeforeSubmit()) {
            pendingQuickSosAfterSmsPermission = true;
            sendSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS);
            return;
        }

        submitQuickSosWithCurrentLocation();
    }

    private void submitQuickSosWithCurrentLocation() {
        viewModel.submitQuickSelfSos(
            currentLatitude,
            currentLongitude,
            currentAccuracy,
            currentCapturedAtMillis != null ? currentCapturedAtMillis : System.currentTimeMillis()
        );
    }

    private void handleSubmitSuccess(@Nullable QuickSosSubmissionResult submissionResult) {
        pendingQuickSos = false;
        pendingQuickSosAfterSmsPermission = false;
        if (submissionResult == null) {
            handleSubmitError(getString(R.string.victim_rescue_submit_error));
            return;
        }

        if (submissionResult.isOnlineCreated()) {
            EmergencyTrackingService.startTracking(requireContext(), submissionResult.getServerSosId().trim());
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_rescue_submit_success), false);
        } else {
            showTopSnackbar(binding.getRoot(), submissionResult.getMessage(), false);
            if (submissionResult.shouldOpenSmsApp()) {
                openSmsAppFallback(submissionResult);
            }
        }
        clearRescueFormAfterSubmit();
    }

    private void handleSubmitError(String message) {
        pendingQuickSos = false;
        pendingQuickSosAfterSmsPermission = false;
        String errorMessage = (message != null && !message.trim().isEmpty())
            ? message
            : getString(R.string.victim_rescue_submit_error);
        showTopSnackbar(binding.getRoot(), errorMessage, true);
    }

    private void clearRescueFormAfterSubmit() {
        binding.etPeopleCount.setText(R.string.victim_rescue_default_people_count);
        binding.etHealthDetail.setText(null);
        selectedImageUris.clear();
        imageAdapter.submitImages(new ArrayList<>(selectedImageUris));
    }

    private void showImageSourceDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.victim_rescue_upload_dialog_title)
            .setItems(R.array.victim_rescue_image_source_options, (dialog, which) -> {
                if (which == 0) {
                    requestCameraAndOpen();
                } else {
                    requestGalleryAndOpen();
                }
            })
            .show();
    }

    private void requestCameraAndOpen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            openCamera();
            return;
        }

        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void requestGalleryAndOpen() {
        String galleryPermission = getGalleryPermission();
        if (galleryPermission == null
            || ContextCompat.checkSelfPermission(requireContext(), galleryPermission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
            return;
        }

        galleryPermissionLauncher.launch(galleryPermission);
    }

    private void openGallery() {
        pickImagesLauncher.launch("image/*");
    }

    private void openCamera() {
        try {
            pendingCameraImageUri = createCameraImageUri();
            takePictureLauncher.launch(pendingCameraImageUri);
        } catch (IOException | IllegalArgumentException exception) {
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_rescue_camera_open_error), true);
        }
    }

    private Uri createCameraImageUri() throws IOException {
        File cameraDir = new File(requireContext().getCacheDir(), "camera");
        if (!cameraDir.exists() && !cameraDir.mkdirs()) {
            throw new IOException("Cannot create camera cache directory");
        }

        File imageFile = File.createTempFile("sos_camera_", ".jpg", cameraDir);
        return FileProvider.getUriForFile(
            requireContext(),
            BuildConfig.APPLICATION_ID + ".fileprovider",
            imageFile
        );
    }

    private void onImageRemoved(Uri imageUri) {
        if (imageUri == null) {
            return;
        }

        String removedUri = imageUri.toString();
        selectedImageUris.removeIf(uri -> uri != null && removedUri.equals(uri.toString()));
        imageAdapter.submitImages(new ArrayList<>(selectedImageUris));
    }

    private void handlePickedImages(List<Uri> imageUris) {
        if (imageUris == null || imageUris.isEmpty()) {
            return;
        }

        boolean reachLimit = false;
        for (Uri imageUri : imageUris) {
            if (imageUri == null) {
                continue;
            }

            if (selectedImageUris.size() >= MAX_SCENE_IMAGES) {
                reachLimit = true;
                break;
            }

            if (!containsUri(selectedImageUris, imageUri)) {
                selectedImageUris.add(imageUri);
            }
        }

        if (reachLimit) {
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_rescue_image_limit_error), true);
        }

        imageAdapter.submitImages(new ArrayList<>(selectedImageUris));
    }

    private void handleTakenPhoto(boolean success) {
        if (!success || pendingCameraImageUri == null) {
            pendingCameraImageUri = null;
            return;
        }

        if (selectedImageUris.size() >= MAX_SCENE_IMAGES) {
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_rescue_image_limit_error), true);
            pendingCameraImageUri = null;
            return;
        }

        if (!containsUri(selectedImageUris, pendingCameraImageUri)) {
            selectedImageUris.add(pendingCameraImageUri);
            imageAdapter.submitImages(new ArrayList<>(selectedImageUris));
        }
        pendingCameraImageUri = null;
    }

    private boolean containsUri(List<Uri> uris, Uri target) {
        if (target == null) {
            return false;
        }

        String targetValue = target.toString();
        for (Uri uri : uris) {
            if (uri != null && targetValue.equals(uri.toString())) {
                return true;
            }
        }
        return false;
    }

    private void setupActivityResultLaunchers() {
        locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                if (isLocationPermissionGranted()) {
                    userLocationManager.startForegroundTracking();
                    fetchCurrentLocation(pendingQuickSos);
                    return;
                }

                showTopSnackbar(binding.getRoot(), getString(R.string.victim_rescue_permission_location_denied), true);
                pendingQuickSos = false;
            }
        );

        cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (Boolean.TRUE.equals(isGranted)) {
                    openCamera();
                } else {
                    showTopSnackbar(binding.getRoot(), getString(R.string.victim_rescue_permission_camera_denied), true);
                }
            }
        );

        galleryPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (Boolean.TRUE.equals(isGranted)) {
                    openGallery();
                } else {
                    showTopSnackbar(binding.getRoot(), getString(R.string.victim_rescue_permission_gallery_denied), true);
                }
            }
        );

        sendSmsPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                boolean shouldSubmit = pendingQuickSosAfterSmsPermission;
                pendingQuickSosAfterSmsPermission = false;
                if (!Boolean.TRUE.equals(isGranted)) {
                    Log.w(SMS_TAG, "SMS_PERMISSION_DENIED");
                }
                if (shouldSubmit && isAdded()) {
                    submitQuickSosWithCurrentLocation();
                }
            }
        );

        takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            this::handleTakenPhoto
        );

        pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            this::handlePickedImages
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
                pendingQuickSos = false;
            }
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_rescue_location_disabled), true);
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
                        currentAccuracy = location.hasAccuracy() ? (double) location.getAccuracy() : null;
                        currentCapturedAtMillis = location.getTime() > 0L
                            ? location.getTime()
                            : System.currentTimeMillis();
                        userLocationManager.updateLocation(location);
                        submitIfPending();
                        return;
                    }

                    requestLastKnownLocation(trySubmitAfterFetch);
                })
                .addOnFailureListener(ignored -> requestLastKnownLocation(trySubmitAfterFetch));
        } catch (SecurityException ignored) {
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_rescue_permission_location_denied), true);
            pendingQuickSos = false;
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
                        currentAccuracy = location.hasAccuracy() ? (double) location.getAccuracy() : null;
                        currentCapturedAtMillis = location.getTime() > 0L
                            ? location.getTime()
                            : System.currentTimeMillis();
                        userLocationManager.updateLocation(location);
                        submitIfPending();
                        return;
                    }

                    if (trySubmitAfterFetch) {
                        pendingQuickSos = false;
                        showTopSnackbar(binding.getRoot(), getString(R.string.victim_rescue_location_unavailable), true);
                    }
                })
                .addOnFailureListener(ignored -> {
                    if (trySubmitAfterFetch) {
                        pendingQuickSos = false;
                        showTopSnackbar(binding.getRoot(), getString(R.string.victim_rescue_location_unavailable), true);
                    }
                });
        } catch (SecurityException ignored) {
            if (trySubmitAfterFetch) {
                pendingQuickSos = false;
                showTopSnackbar(binding.getRoot(), getString(R.string.victim_rescue_permission_location_denied), true);
            }
        }
    }

    private void submitIfPending() {
        if (!pendingQuickSos || currentLatitude == null || currentLongitude == null) {
            return;
        }

        pendingQuickSos = false;
        submitQuickSos();
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

    private void syncCurrentLocationFromCache() {
        if (!userLocationManager.hasLocationPermission()) {
            return;
        }

        UserLocationManager.LocationSnapshot snapshot = userLocationManager.getFreshLocation(
            UserLocationManager.QUICK_SOS_FRESH_LOCATION_MAX_AGE_MS
        );
        if (snapshot == null) {
            return;
        }

        currentLatitude = snapshot.getLatitude();
        currentLongitude = snapshot.getLongitude();
        currentAccuracy = snapshot.getAccuracy();
        currentCapturedAtMillis = snapshot.getCapturedAtMillis();
    }

    private boolean shouldRequestSendSmsPermissionBeforeSubmit() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        if (networkMonitor.hasInternet()) {
            return false;
        }
        if (!requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)) {
            Log.w(SMS_TAG, "SMS_DEVICE_NOT_SUPPORTED");
            return false;
        }
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED;
    }

    private void openSmsAppFallback(@Nullable QuickSosSubmissionResult submissionResult) {
        if (submissionResult == null
            || submissionResult.getGatewayPhoneNumber() == null
            || submissionResult.getGatewayPhoneNumber().trim().isEmpty()
            || submissionResult.getSmsBody() == null
            || submissionResult.getSmsBody().trim().isEmpty()) {
            return;
        }

        try {
            Intent intent = new Intent(
                Intent.ACTION_SENDTO,
                Uri.parse("smsto:" + Uri.encode(submissionResult.getGatewayPhoneNumber().trim()))
            );
            intent.putExtra("sms_body", submissionResult.getSmsBody());
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private String getGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        }
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

}
