package com.drc.aidbridge.ui.main.fragment.victim;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVictimPersonalInfoBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimPersonalInfoFragment extends BaseFragment<FragmentVictimPersonalInfoBinding> {

    @Nullable
    @Override
    protected FragmentVictimPersonalInfoBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimPersonalInfoBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        binding.btnBack.setOnClickListener(v -> popBackStackSafely());

        binding.btnUpdateInfo.setOnClickListener(v -> Toast.makeText(
                requireContext(),
                getString(R.string.victim_personal_info_toast_update_success),
                Toast.LENGTH_SHORT
        ).show());

        binding.btnChangePassword.setOnClickListener(v -> Toast.makeText(
                requireContext(),
                getString(R.string.victim_personal_info_toast_change_password_success),
                Toast.LENGTH_SHORT
        ).show());
    }

    @Override
    protected void observeViewModel() {
        // TODO: Observe ViewModel state when victim profile use case is implemented.
    }
}
