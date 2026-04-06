package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerPersonalInfoBinding;
import com.drc.aidbridge.domain.model.volunteer.VolunteerPersonalInfo;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerDashboardViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerPersonalInfoFragment extends BaseFragment<FragmentVolunteerPersonalInfoBinding> {

	private VolunteerDashboardViewModel volunteerDashboardViewModel;

	@Nullable
	@Override
	protected FragmentVolunteerPersonalInfoBinding inflateBinding(LayoutInflater inflater,
			@Nullable ViewGroup container) {
		return FragmentVolunteerPersonalInfoBinding.inflate(inflater, container, false);
	}

	@Override
	protected void setupViews() {
		volunteerDashboardViewModel = new ViewModelProvider(requireActivity()).get(VolunteerDashboardViewModel.class);
		mockProfileData();
		setupClickListeners();
	}

	@Override
	protected void observeViewModel() {
		if (volunteerDashboardViewModel == null) {
			return;
		}

		volunteerDashboardViewModel.getVolunteerPersonalInfoResult().observe(
				getViewLifecycleOwner(),
				resultObserver(this::renderPersonalInfo, this::showToast));
	}

	private void renderPersonalInfo(@Nullable VolunteerPersonalInfo info) {
		if (info == null) {
			return;
		}

		String fullName = info.getFullName() != null && !info.getFullName().trim().isEmpty()
				? info.getFullName().trim()
				: getString(R.string.volunteer_personal_info_default_full_name);
		String phoneNumber = info.getPhoneNumber() != null && !info.getPhoneNumber().trim().isEmpty()
				? info.getPhoneNumber().trim()
				: getString(R.string.volunteer_personal_info_default_phone);
		String email = info.getEmail() != null && !info.getEmail().trim().isEmpty()
				? info.getEmail().trim()
				: getString(R.string.volunteer_personal_info_default_email);

		binding.etFullName.setText(fullName);
		binding.etPhone.setText(phoneNumber);
		binding.etEmail.setText(email);
	}

	private void setupClickListeners() {
		binding.btnBack.setOnClickListener(v -> popBackStackSafely());

		binding.btnUpdateInfo
				.setOnClickListener(v -> showToast(getString(R.string.volunteer_personal_info_toast_update_success)));

		binding.btnChangePassword.setOnClickListener(v -> {
			String newPassword = String.valueOf(binding.etNewPassword.getText()).trim();
			String confirmPassword = String.valueOf(binding.etConfirmNewPassword.getText()).trim();

			if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
				showToast(getString(R.string.volunteer_personal_info_toast_change_password_empty));
				return;
			}

			if (newPassword.equals(confirmPassword)) {
				showToast(getString(R.string.volunteer_personal_info_toast_change_password_success));
			} else {
				showToast(getString(R.string.volunteer_personal_info_toast_change_password_mismatch));
			}
		});
	}

	private void mockProfileData() {
		binding.etFullName.setText(getString(R.string.volunteer_personal_info_default_full_name));
		binding.etPhone.setText(getString(R.string.volunteer_personal_info_default_phone));
		binding.etEmail.setText(getString(R.string.volunteer_personal_info_default_email));
	}
}
