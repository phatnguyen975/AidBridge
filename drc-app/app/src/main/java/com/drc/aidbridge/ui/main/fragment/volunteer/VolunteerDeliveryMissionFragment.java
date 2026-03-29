package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerDeliveryMissionBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerTaskViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerDeliveryMissionFragment extends BaseFragment<FragmentVolunteerDeliveryMissionBinding> {

    private static final int TOTAL_STEPS = 4;

    private int currentStep = 1;
    private VolunteerTaskViewModel volunteerTaskViewModel;

    private View[] progressSegments;
    private MaterialCardView[] timelineIconCards;
    private View[] timelineConnectors;
    private TextView[] timelineTitles;
    private TextView[] timelineDescriptions;

    @Override
    protected FragmentVolunteerDeliveryMissionBinding inflateBinding(LayoutInflater inflater,
            @Nullable ViewGroup container) {
        return FragmentVolunteerDeliveryMissionBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        volunteerTaskViewModel = new ViewModelProvider(requireActivity()).get(VolunteerTaskViewModel.class);
        initUiReferences();
        mockDeliveryData();
        setupClickListeners();
        updateUIByStep();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isAdded() || getActivity() == null) {
            return;
        }

        BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            MenuItem missionItem = bottomNav.getMenu().findItem(R.id.volunteerMissionListFragment);
            if (missionItem != null) {
                missionItem.setChecked(true);
            }
        }
    }

    @Override
    protected void observeViewModel() {
        // TODO: Observe API-backed state when delivery workflow is integrated.
    }

    private void initUiReferences() {
        progressSegments = new View[] {
                binding.viewProgressSegment1,
                binding.viewProgressSegment2,
                binding.viewProgressSegment3,
                binding.viewProgressSegment4
        };

        timelineIconCards = new MaterialCardView[] {
                binding.cardTimelineStep1Icon,
                binding.cardTimelineStep2Icon,
                binding.cardTimelineStep3Icon,
                binding.cardTimelineStep4Icon
        };

        timelineConnectors = new View[] {
                binding.viewTimelineConnector1,
                binding.viewTimelineConnector2,
                binding.viewTimelineConnector3
        };

        timelineTitles = new TextView[] {
                binding.tvTimelineStep1Title,
                binding.tvTimelineStep2Title,
                binding.tvTimelineStep3Title,
                binding.tvTimelineStep4Title
        };

        timelineDescriptions = new TextView[] {
                binding.tvTimelineStep1Desc,
                binding.tvTimelineStep2Desc,
                binding.tvTimelineStep3Desc,
                binding.tvTimelineStep4Desc
        };
    }

    private void setupClickListeners() {
        binding.btnAction.setOnClickListener(v -> {
            if (currentStep >= TOTAL_STEPS) {
                finishDeliveryMission();
                return;
            }

            currentStep++;
            updateUIByStep();
        });

        binding.btnSeeDetails.setOnClickListener(
                v -> showToast(getString(R.string.volunteer_delivery_mission_toast_detail_todo)));
    }

    private void updateUIByStep() {
        int activeColor = ContextCompat.getColor(requireContext(), R.color.volunteer_acceptance_accept_button_bg);
        int inactiveColor = ContextCompat.getColor(requireContext(),
                R.color.volunteer_acceptance_decline_button_stroke);
        int inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.volunteer_acceptance_text_secondary);

        binding.tvDeliveryProgress.setText(String.format(Locale.getDefault(), "%d/4 TIẾN ĐỘ", currentStep));

        for (int i = 0; i < progressSegments.length; i++) {
            progressSegments[i].setBackgroundColor(i < currentStep ? activeColor : inactiveColor);
        }

        for (int i = 0; i < timelineIconCards.length; i++) {
            boolean isActive = i < currentStep;
            timelineIconCards[i].setCardBackgroundColor(isActive ? activeColor : inactiveColor);
            timelineTitles[i].setTextColor(isActive ? activeColor : inactiveTextColor);
            timelineDescriptions[i].setAlpha(isActive ? 1f : 0.65f);
        }

        for (int i = 0; i < timelineConnectors.length; i++) {
            timelineConnectors[i].setBackgroundColor(i < currentStep - 1 ? activeColor : inactiveColor);
        }

        switch (currentStep) {
            case 1:
                binding.btnAction.setText(R.string.volunteer_delivery_mission_btn_arrived_depot);
                break;
            case 2:
                binding.btnAction.setText(R.string.volunteer_delivery_mission_btn_loaded_items);
                break;
            case 3:
                binding.btnAction.setText(R.string.volunteer_delivery_mission_btn_arrived_victim_home);
                break;
            default:
                binding.btnAction.setText(R.string.volunteer_delivery_mission_btn_completed);
                break;
        }
    }

    private void finishDeliveryMission() {
        volunteerTaskViewModel.completeMission();
        showToast(getString(R.string.volunteer_delivery_mission_toast_completed));
        navigateSafely(R.id.action_delivery_mission_to_dashboard);
    }

    private void mockDeliveryData() {
        binding.tvRecipientNameValue.setText(R.string.volunteer_delivery_mission_recipient_name_value);
        binding.tvRecipientPhoneValue.setText(R.string.volunteer_delivery_mission_recipient_phone_value);
        binding.tvRecipientNoteValue.setText(R.string.volunteer_delivery_mission_recipient_note_value);
        binding.tvOrderDetailTitle.setText(R.string.volunteer_delivery_mission_order_detail_title);
    }
}
