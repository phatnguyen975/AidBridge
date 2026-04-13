package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerMissionBinding;
import com.drc.aidbridge.domain.model.VolunteerMission;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerTaskViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerMissionFragment extends BaseFragment<FragmentVolunteerMissionBinding> {

    private VolunteerTaskViewModel volunteerTaskViewModel;
    private Integer lastRoutedId;

    @Nullable
    @Override
    protected FragmentVolunteerMissionBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVolunteerMissionBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        volunteerTaskViewModel = new ViewModelProvider(requireActivity()).get(VolunteerTaskViewModel.class);
        showRouterLoading(true);
        evaluateMissionRouter();
    }

    @Override
    protected void observeViewModel() {
        volunteerTaskViewModel.getIsMissionAccepted().observe(getViewLifecycleOwner(),
                isAccepted -> evaluateMissionRouter());
        volunteerTaskViewModel.getCurrentMissionType().observe(getViewLifecycleOwner(),
                missionType -> evaluateMissionRouter());
        volunteerTaskViewModel.getPendingMission().observe(getViewLifecycleOwner(),
                mission -> evaluateMissionRouter());
    }

    private void evaluateMissionRouter() {
        boolean missionAccepted = Boolean.TRUE.equals(volunteerTaskViewModel.getIsMissionAccepted().getValue());
        String missionType = volunteerTaskViewModel.getCurrentMissionType().getValue();
        VolunteerMission pendingMission = volunteerTaskViewModel.getPendingMission().getValue();

        if (!missionAccepted && pendingMission != null) {
            showRouterLoading(true);
            routeByDestinationIfNeeded(R.id.volunteerSosAcceptanceFragment);
            return;
        }

        if (!missionAccepted) {
            lastRoutedId = null;
            showRouterLoading(false);
            binding.tvMissionPlaceholder.setVisibility(View.VISIBLE);
            binding.tvMissionRouterHint.setVisibility(View.VISIBLE);
            return;
        }

        if (VoluteerMissionAcceptanceFragment.MISSION_TYPE_RESCUE.equalsIgnoreCase(missionType)) {
            showRouterLoading(true);
            routeByActionIfNeeded(R.id.action_mission_list_to_current_sos_mission);
            return;
        }

        if (VoluteerMissionAcceptanceFragment.isDeliveryMissionType(missionType)) {
            showRouterLoading(true);
            routeByActionIfNeeded(R.id.action_mission_list_to_delivery_mission);
            return;
        }

        // Keep loading visible while waiting for mission type hydration to avoid UI
        // flicker.
        showRouterLoading(true);
    }

    private void routeByActionIfNeeded(int actionId) {
        if (lastRoutedId != null && lastRoutedId == actionId) {
            return;
        }

        lastRoutedId = actionId;
        navigateSafely(actionId);
    }

    private void routeByDestinationIfNeeded(int destinationId) {
        if (lastRoutedId != null && lastRoutedId == destinationId) {
            return;
        }

        lastRoutedId = destinationId;
        navigateToDestinationSafely(destinationId);
    }

    private void showRouterLoading(boolean isLoading) {
        binding.progressMissionRouter.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.tvMissionPlaceholder.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        binding.tvMissionRouterHint.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }
}
