package com.drc.aidbridge.ui.map.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.domain.model.VolunteerMission;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerMapViewModel;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerTaskViewModel;
import com.drc.aidbridge.ui.map.base.BaseMapFragment;

import org.osmdroid.util.GeoPoint;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerMapFragment extends BaseMapFragment<VolunteerMapViewModel> {

    private VolunteerMapViewModel volunteerMapViewModel;
    private VolunteerTaskViewModel volunteerTaskViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize ViewModels early so they are available in setupViews()
        volunteerMapViewModel = new ViewModelProvider(requireActivity()).get(VolunteerMapViewModel.class);
        volunteerTaskViewModel = new ViewModelProvider(requireActivity()).get(VolunteerTaskViewModel.class);
    }

    @Override
    protected VolunteerMapViewModel getViewModel() {
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
        // Any additional setup for VolunteerMapFragment can go here
    }

    @Override
    protected void observeViewModel() {
        super.observeViewModel();
        volunteerTaskViewModel.getPendingMission().observe(getViewLifecycleOwner(), this::bindMissionData);
    }

    private void bindMissionData(@Nullable VolunteerMission mission) {
        if (mission != null) {
            if (mission.getVictimLat() != null && mission.getVictimLng() != null && endPoint == null) {
                endPoint = new GeoPoint(mission.getVictimLat(), mission.getVictimLng());
                volunteerMapViewModel.setEndPoint(endPoint);
            }
        }

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
