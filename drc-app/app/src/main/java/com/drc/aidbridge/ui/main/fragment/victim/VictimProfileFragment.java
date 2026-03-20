package com.drc.aidbridge.ui.main.fragment.victim;

import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVictimProfileBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimProfileFragment extends BaseFragment<FragmentVictimProfileBinding> {

    @Nullable
    @Override
    protected FragmentVictimProfileBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        setupClickListeners();
    }

    @Override
    protected void observeViewModel() {
        // TODO: Observe ViewModel state once profile/logout use cases are implemented.
    }

    private void setupClickListeners() {
        binding.cardTrackJourney.setOnClickListener(v ->
            navigateSafely(R.id.action_profile_to_map));

        binding.cardRequestHistory.setOnClickListener(v ->
            navigateSafely(R.id.action_profile_to_history));

        binding.rowPersonalInfo.setOnClickListener(v ->
            navigateSafely(R.id.action_profile_to_personalInfo));

        binding.rowLogout.setOnClickListener(v ->
            Toast.makeText(requireContext(),
                "TODO: Call ViewModel to logout",
                Toast.LENGTH_SHORT).show());
    }
}
