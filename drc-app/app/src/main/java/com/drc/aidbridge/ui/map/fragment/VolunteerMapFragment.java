package com.drc.aidbridge.ui.map.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.domain.model.VolunteerMission;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerMapViewModel;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerTaskViewModel;
import com.drc.aidbridge.ui.map.base.BaseMapFragment;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerMapFragment extends BaseMapFragment<VolunteerMapViewModel> {

    private VolunteerMapViewModel volunteerMapViewModel;
    private VolunteerTaskViewModel volunteerTaskViewModel;
    private Marker victimMarker;
    private android.view.View victimPulseView;
    private android.animation.ValueAnimator victimPulseAnimator;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize ViewModels early so they are available in setupViews()
        volunteerMapViewModel = new ViewModelProvider(this).get(VolunteerMapViewModel.class);
        volunteerTaskViewModel = new ViewModelProvider(requireActivity()).get(VolunteerTaskViewModel.class);
    }

    @Override
    protected VolunteerMapViewModel getViewModel() {
        if (volunteerMapViewModel == null) {
            volunteerMapViewModel = new ViewModelProvider(this).get(VolunteerMapViewModel.class);
        }
        return volunteerMapViewModel;
    }

    @Override
    protected int getContentLayout() {
        return R.layout.fragment_map_volunteer;
    }

    @Override
    protected void setupRoleSpecificUI() {
        bindMissionData(volunteerTaskViewModel.getPendingMission().getValue());
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        volunteerTaskViewModel.fetchCurrentMission();
        binding.btnSetEndPoint.setOnClickListener(v -> showEndPointSelectionMenu());
    }

    @Override
    protected void observeViewModel() {
        super.observeViewModel();
        volunteerTaskViewModel.getPendingMission().observe(getViewLifecycleOwner(), this::bindMissionData);
        
        volunteerTaskViewModel.getCurrentMissionResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullItemDto data = result.getData();
                if (data != null) {
                    Double victimLat = data.getVictimLat();
                    Double victimLng = data.getVictimLng();
                    
                    if (victimLat == null && data.getSosRequestDetail() != null) {
                        victimLat = data.getSosRequestDetail().getLat();
                        victimLng = data.getSosRequestDetail().getLng();
                    }

                    if (victimLat != null && victimLng != null) {
                        GeoPoint victimPoint = new GeoPoint(victimLat, victimLng);
                        updateVictimMarker(victimPoint);
                    }
                }
            }
        });
    }

    private void showEndPointSelectionMenu() {
        com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullItemDto currentMission = 
            volunteerTaskViewModel.getCurrentMissionResult().getValue() != null ? 
            volunteerTaskViewModel.getCurrentMissionResult().getValue().getData() : null;

        Double victimLat = currentMission != null ? currentMission.getVictimLat() : null;
        Double victimLng = currentMission != null ? currentMission.getVictimLng() : null;

        if (victimLat == null && currentMission != null && currentMission.getSosRequestDetail() != null) {
            victimLat = currentMission.getSosRequestDetail().getLat();
            victimLng = currentMission.getSosRequestDetail().getLng();
        }

        if (victimLat == null || victimLng == null) {
            setPointSelectionMode(PointSelectionMode.END);
            return;
        }

        final double finalLat = victimLat;
        final double finalLng = victimLng;

        String[] options = {"Chọn tự do trên bản đồ", "Chọn vị trí của nạn nhân"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chọn điểm đến")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        setPointSelectionMode(PointSelectionMode.END);
                    } else {
                        endPoint = new GeoPoint(finalLat, finalLng);
                        volunteerMapViewModel.setEndPoint(endPoint);
                        reverseGeocodeAsync(endPoint, false);
                        updateMapMarkers();
                        showToast("Đã chọn vị trí của nạn nhân làm điểm đến.");
                    }
                })
                .show();
    }

    private void updateVictimMarker(@Nullable GeoPoint victimPoint) {
        if (mapView == null) {
            return;
        }
        
        if (victimMarker != null) {
            mapView.getOverlays().remove(victimMarker);
            victimMarker = null;
        }
        
        if (victimPoint != null) {
            victimMarker = new Marker(mapView);
            victimMarker.setPosition(victimPoint);
            victimMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            victimMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_victim_location_pin));
            victimMarker.setTitle("Vị trí nạn nhân");
            victimMarker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);
            victimMarker.setOnMarkerClickListener((clickedMarker, clickedMapView) -> {
                clickedMarker.showInfoWindow();
                return true;
            });
            mapView.getOverlays().add(victimMarker);

            if (victimPulseView == null) {
                victimPulseView = android.view.LayoutInflater.from(requireContext())
                        .inflate(R.layout.view_pulse_victim, mapView, false);
                
                android.view.View locationPin = victimPulseView.findViewById(R.id.imgVictimPin);
                if (locationPin != null) {
                    locationPin.post(() -> {
                        locationPin.setPivotX(locationPin.getWidth() / 2f);
                        locationPin.setPivotY(locationPin.getHeight());
                    });
                }
                victimPulseAnimator = android.animation.ValueAnimator.ofFloat(0.8f, 1.1f);
                victimPulseAnimator.setDuration(1000);
                victimPulseAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                victimPulseAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                victimPulseAnimator.addUpdateListener(animation -> {
                    if (locationPin != null) {
                        float animatedValue = (float) animation.getAnimatedValue();
                        locationPin.setScaleX(animatedValue);
                        locationPin.setScaleY(animatedValue);
                    }
                });
                victimPulseAnimator.start();

                org.osmdroid.views.MapView.LayoutParams params = new org.osmdroid.views.MapView.LayoutParams(
                        org.osmdroid.views.MapView.LayoutParams.WRAP_CONTENT,
                        org.osmdroid.views.MapView.LayoutParams.WRAP_CONTENT,
                        victimPoint,
                        org.osmdroid.views.MapView.LayoutParams.BOTTOM_CENTER,
                        0, 0
                );
                mapView.addView(victimPulseView, params);
            } else {
                org.osmdroid.views.MapView.LayoutParams params = 
                        (org.osmdroid.views.MapView.LayoutParams) victimPulseView.getLayoutParams();
                params.geoPoint = victimPoint;
                victimPulseView.setLayoutParams(params);
            }
        } else {
            if (victimPulseView != null) {
                if (victimPulseAnimator != null) {
                    victimPulseAnimator.cancel();
                    victimPulseAnimator = null;
                }
                mapView.removeView(victimPulseView);
                victimPulseView = null;
            }
        }
        mapView.invalidate();
    }

    private void bindMissionData(@Nullable VolunteerMission mission) {
        reverseGeocodeAsync(endPoint, false);

        if (isMapScreenActive) {
            try {
                updateMapMarkers();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Override
    protected void openQuickDial() {
        VolunteerMission mission = volunteerTaskViewModel.getPendingMission().getValue();
        String candidatePhone = mission != null ? extractPhoneNumber(mission.getComment()) : null;
        if (candidatePhone == null || candidatePhone.trim().isEmpty()) {
            candidatePhone = getString(R.string.base_map_call_fallback_number);
        }

        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + candidatePhone));
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @Override
    public void onDestroyView() {
        if (victimPulseAnimator != null) {
            victimPulseAnimator.cancel();
            victimPulseAnimator = null;
        }
        super.onDestroyView();
    }

    @Nullable
    private String extractPhoneNumber(@Nullable String source) {
        if (source == null) {
            return null;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\+?\\d[\\d\\s.-]{8,}\\d)");
        java.util.regex.Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("[^\\d+]", "");
        }
        return null;
    }
}
