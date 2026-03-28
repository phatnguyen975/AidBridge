package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.MenuItem;

import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerCurrentSosMissionBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerCurrentSosMissionFragment extends BaseFragment<FragmentVolunteerCurrentSosMissionBinding> {

    @Override
    protected FragmentVolunteerCurrentSosMissionBinding inflateBinding(LayoutInflater inflater,
            @Nullable ViewGroup container) {
        return FragmentVolunteerCurrentSosMissionBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        syncBottomNavigationSelection();
        mockVictimData();
        setupClickListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        syncBottomNavigationSelection();
    }

    @Override
    protected void observeViewModel() {
        // TODO: Observe ViewModel state when API is integrated.
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> popBackStackSafely());

        binding.btnCallVictim.setOnClickListener(v -> openDialer());

        binding.btnArrived.setOnClickListener(v -> {
            showToast(getString(R.string.volunteer_current_mission_toast_arrived));
            // TODO: Điều hướng sang màn hình Xác nhận cứu hộ sau khi tích hợp API.
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
