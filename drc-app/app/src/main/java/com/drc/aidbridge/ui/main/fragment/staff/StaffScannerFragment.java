package com.drc.aidbridge.ui.main.fragment.staff;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentStaffScannerBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffScannerFragment extends BaseFragment<FragmentStaffScannerBinding> {

    private static final String TAG = "StaffScannerFragment";
    private static final String ARG_MODE = "mode";
    private static final String ARG_CODE = "code";
    private static final String MODE_IMPORT = "import";
    private static final String MODE_EXPORT = "export";

    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private ProcessCameraProvider cameraProvider;
    private String mode = MODE_EXPORT;
    private boolean hasScanned;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();
        barcodeScanner = BarcodeScanning.getClient(
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
        );
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        startCamera();
                    } else {
                        showToast("Vui l\u00f2ng c\u1ea5p quy\u1ec1n camera ho\u1eb7c nh\u1eadp m\u00e3 th\u1ee7 c\u00f4ng.");
                    }
                }
        );
    }

    @Nullable
    @Override
    protected FragmentStaffScannerBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentStaffScannerBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        mode = resolveMode();
        bindModeUi();

        binding.ivBack.setOnClickListener(v -> popBackStackSafely());
        binding.btnManualEntry.setOnClickListener(v -> showManualEntryBottomSheet());
        if (MODE_EXPORT.equals(mode)) {
            showManualEntryBottomSheet();
            return;
        }
        requestCameraOrStart();
    }

    @Override
    protected void observeViewModel() {
    }

    @Override
    public void onDestroyView() {
        stopCamera();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        super.onDestroy();
    }

    private String resolveMode() {
        Bundle args = getArguments();
        String argMode = args != null ? args.getString(ARG_MODE, MODE_EXPORT) : MODE_EXPORT;
        return MODE_IMPORT.equals(argMode) ? MODE_IMPORT : MODE_EXPORT;
    }

    private void bindModeUi() {
        if (MODE_IMPORT.equals(mode)) {
            binding.tvToolbarTitle.setText(R.string.staff_scan_title_import);
            binding.tvInstruction.setText(R.string.staff_scan_inst_import);
            return;
        }
        binding.tvToolbarTitle.setText(R.string.staff_scan_title_export);
        binding.tvInstruction.setText(R.string.staff_scan_inst_export);
    }

    private void requestCameraOrStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
            return;
        }
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startCamera() {
        if (!isAdded() || binding == null) {
            return;
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                if (!isAdded() || binding == null) {
                    return;
                }
                cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                );
            } catch (Exception exception) {
                Log.e(TAG, "Unable to start camera", exception);
                showToast("Kh\u00f4ng th\u1ec3 m\u1edf camera. Vui l\u00f2ng nh\u1eadp m\u00e3 th\u1ee7 c\u00f4ng.");
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (hasScanned) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );
        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null && !rawValue.trim().isEmpty()) {
                            handleScannedCode(rawValue.trim());
                            return;
                        }
                    }
                })
                .addOnFailureListener(error -> Log.w(TAG, "QR decode failed", error))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void handleScannedCode(@NonNull String code) {
        if (hasScanned || !isAdded() || binding == null) {
            return;
        }
        hasScanned = true;
        binding.getRoot().post(() -> {
            stopCamera();
            navigateToDetail(code);
        });
    }

    private void navigateToDetail(@NonNull String code) {
        Bundle args = new Bundle();
        args.putString(ARG_CODE, code);
        args.putString(ARG_MODE, mode);
        int actionId = MODE_IMPORT.equals(mode)
                ? R.id.action_staffScannerFragment_to_staffImportDetailFragment
                : R.id.action_staffScannerFragment_to_staffExportDetailFragment;
        navigateSafely(actionId, args);
    }

    private void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    private void showManualEntryBottomSheet() {
        StaffManualEntryBottomSheet.newInstance(mode)
                .show(getChildFragmentManager(), StaffManualEntryBottomSheet.class.getSimpleName());
    }
}
