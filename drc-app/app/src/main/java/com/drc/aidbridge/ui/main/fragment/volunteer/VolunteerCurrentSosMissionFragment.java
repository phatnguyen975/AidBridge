package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerCurrentSosMissionBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerTaskViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerCurrentSosMissionFragment extends BaseFragment<FragmentVolunteerCurrentSosMissionBinding> {

    private VolunteerTaskViewModel volunteerTaskViewModel;

    @Override
    protected FragmentVolunteerCurrentSosMissionBinding inflateBinding(LayoutInflater inflater,
            @Nullable ViewGroup container) {
        return FragmentVolunteerCurrentSosMissionBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        volunteerTaskViewModel = new ViewModelProvider(requireActivity()).get(VolunteerTaskViewModel.class);
        syncBottomNavigationSelection();
        mockVictimData();
        volunteerTaskViewModel.fetchCurrentMission();
        setupClickListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottom_nav);
        if (bottomNavigationView != null) {
            MenuItem missionItem = bottomNavigationView.getMenu().findItem(R.id.volunteerMissionListFragment);
            if (missionItem != null) {
                missionItem.setChecked(true);
            }
        }
    }

    @Override
    protected void observeViewModel() {
        volunteerTaskViewModel.getCurrentMissionResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullItemDto data = result.getData();
                if (data != null) {
                    String codeName = data.getCodeName();
                    binding.tvMissionCodeValue.setText(
                            (codeName == null || codeName.trim().isEmpty()) ? "Ma nhiem vu: N/A" : "Ma nhiem vu: " + codeName.trim()
                    );
                    if (data.getVictimLat() != null && data.getVictimLng() != null) {
                        binding.tvVictimLocation.setText(String.format("Tọa độ: %.6f, %.6f", data.getVictimLat(), data.getVictimLng()));
                    } else if (data.getSosRequestDetail() != null && data.getSosRequestDetail().getLat() != null) {
                        binding.tvVictimLocation.setText(String.format("Tọa độ: %.6f, %.6f", data.getSosRequestDetail().getLat(), data.getSosRequestDetail().getLng()));
                    } else {
                        binding.tvVictimLocation.setText("Tọa độ: N/A");
                    }

                    if (data.getRadiusKm() != null) {
                        binding.tvRadiusKm.setText(String.format("Bán kính cứu trợ: %.2f km", data.getRadiusKm()));
                    } else {
                        binding.tvRadiusKm.setText("Bán kính cứu trợ: N/A");
                    }

                    if (data.getAddress() != null) {
                        binding.tvVictimAddress.setText(data.getAddress());
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        binding.btnCallVictim.setOnClickListener(v -> openDialer());

        binding.btnArrived.setOnClickListener(v -> {
            volunteerTaskViewModel.completeMission();
            showToast(getString(R.string.volunteer_current_mission_toast_completed));
            navigateSafely(R.id.action_current_sos_mission_to_dashboard);
        });
    }

    private void syncBottomNavigationSelection() {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottom_nav);
        if (bottomNavigationView == null) {
            return;
        }

        MenuItem missionItem = bottomNavigationView.getMenu().findItem(R.id.volunteerMissionListFragment);
        if (missionItem != null) {
            missionItem.setChecked(true);
        }
    }

    private void openDialer() {
        String rawPhone = binding.tvVictimPhone.getText() != null
                ? binding.tvVictimPhone.getText().toString().trim()
                : "";

        if (rawPhone.isEmpty()) {
            return;
        }

        String dialPhone = rawPhone.replace(" ", "");
        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + dialPhone));
        try {
            startActivity(dialIntent);
        } catch (ActivityNotFoundException exception) {
            showToast(getString(R.string.volunteer_current_mission_dialer_not_found));
        }
    }

    private void mockVictimData() {
        binding.tvVictimName.setText(R.string.volunteer_current_mission_victim_name);
        binding.tvVictimPhone.setText(R.string.volunteer_current_mission_victim_phone);
        binding.tvVictimAddress.setText(R.string.volunteer_current_mission_victim_address);
        binding.tvImportantNoteValue.setText(R.string.volunteer_current_mission_important_note_value);
    }
}
