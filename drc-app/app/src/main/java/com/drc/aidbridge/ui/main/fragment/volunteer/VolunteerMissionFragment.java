package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerMissionBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerTaskViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerMissionFragment extends BaseFragment<FragmentVolunteerMissionBinding> {

    private VolunteerTaskViewModel volunteerTaskViewModel;
    private boolean hasRoutedToCurrentMission;

    @Nullable
    @Override
    protected FragmentVolunteerMissionBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVolunteerMissionBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        volunteerTaskViewModel = new ViewModelProvider(requireActivity()).get(VolunteerTaskViewModel.class);
        showRouterLoading(true);

        Boolean currentAcceptedState = volunteerTaskViewModel.getIsMissionAccepted().getValue();
        if (currentAcceptedState != null) {
            handleMissionAcceptedState(currentAcceptedState);
        }
    }

    @Override
    protected void observeViewModel() {
        volunteerTaskViewModel.getIsMissionAccepted().observe(getViewLifecycleOwner(),
                this::handleMissionAcceptedState);
    }

    private void handleMissionAcceptedState(@Nullable Boolean isAccepted) {
        boolean missionAccepted = Boolean.TRUE.equals(isAccepted);
        if (missionAccepted) {
            showRouterLoading(true);
            routeToCurrentMissionIfNeeded();
            return;
        }

        hasRoutedToCurrentMission = false;
        showRouterLoading(false);
        binding.tvMissionPlaceholder.setVisibility(View.VISIBLE);
        binding.tvMissionRouterHint.setVisibility(View.VISIBLE);
    }

    private void routeToCurrentMissionIfNeeded() {
        if (hasRoutedToCurrentMission) {
            return;
        }

        hasRoutedToCurrentMission = true;
        navigateSafely(R.id.action_mission_list_to_current_sos_mission);
    }

    private void showRouterLoading(boolean isLoading) {
        binding.progressMissionRouter.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
