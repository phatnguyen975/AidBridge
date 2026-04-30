package com.drc.aidbridge.ui.main.fragment.admin;

import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminAddStaffBinding;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.model.admin.Staff;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminAddStaffViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminAddStaffFragment extends BaseFragment<FragmentAdminAddStaffBinding> {

    private AdminAddStaffViewModel viewModel;
    private ArrayAdapter<HubSpinnerItem> hubAdapter;
    private String selectedHubId;
    private boolean hubsLoaded;

    @Nullable
    @Override
    protected FragmentAdminAddStaffBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAdminAddStaffBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AdminAddStaffViewModel.class);
        setupHubSpinner();
        binding.buttonAdminAddStaffBack.setOnClickListener(v -> popBackStackSafely());
        binding.buttonCreateStaff.setOnClickListener(v -> submitStaff());
        binding.buttonCreateStaff.setEnabled(false);
        viewModel.loadHubs();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getHubsResult().observe(
                getViewLifecycleOwner(),
                resultObserver(this::renderHubs, this::renderHubLoadError)
        );

        viewModel.getCreateStaffResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) {
                return;
            }

            boolean isLoading = result.isLoading();
            applySubmitState(isLoading);

            if (result.hasBeenHandled() && !isLoading) {
                return;
            }

            if (result.isSuccess()) {
                result.markAsHandled();
                handleCreateStaffSuccess(result.getData());
            } else if (result.isError()) {
                result.markAsHandled();
                String message = result.getMessage();
                showToast(message != null && !message.trim().isEmpty()
                        ? message
                        : getString(R.string.admin_add_staff_error_generic));
            }
        });
    }

    private void setupHubSpinner() {
        hubAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_admin_spinner_text,
                new ArrayList<>()
        );
        hubAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerStaffHub.setAdapter(hubAdapter);
        binding.spinnerStaffHub.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                HubSpinnerItem selected = hubAdapter.getItem(position);
                selectedHubId = selected != null ? selected.hubId : null;
                if (selectedHubId != null) {
                    binding.textAdminAddStaffHubError.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedHubId = null;
            }
        });
    }

    private void renderHubs(@Nullable List<Hub> hubs) {
        selectedHubId = null;
        hubAdapter.clear();
        hubAdapter.add(new HubSpinnerItem(null, getString(R.string.admin_add_staff_hub_placeholder)));

        if (hubs != null) {
            for (Hub hub : hubs) {
                if (hub == null || hub.getId() == null) {
                    continue;
                }
                hubAdapter.add(new HubSpinnerItem(
                        hub.getId().toString(),
                        resolveHubName(hub)
                ));
            }
        }
        hubAdapter.notifyDataSetChanged();
        hubsLoaded = hubAdapter.getCount() > 1;
        binding.buttonCreateStaff.setEnabled(hubsLoaded);
        if (!hubsLoaded) {
            binding.textAdminAddStaffHubError.setText(R.string.admin_add_staff_hub_empty);
            binding.textAdminAddStaffHubError.setVisibility(View.VISIBLE);
        }
    }

    private void renderHubLoadError(@NonNull String message) {
        hubsLoaded = false;
        selectedHubId = null;
        binding.buttonCreateStaff.setEnabled(false);
        binding.textAdminAddStaffHubError.setText(
                message.trim().isEmpty()
                        ? getString(R.string.admin_add_staff_hub_load_error)
                        : message
        );
        binding.textAdminAddStaffHubError.setVisibility(View.VISIBLE);
    }

    private void submitStaff() {
        String fullName = readText(binding.editTextStaffFullName);
        String email = readText(binding.editTextStaffEmail);
        String phone = readText(binding.editTextStaffPhone);
        String password = readText(binding.editTextStaffPassword);
        String confirmPassword = readText(binding.editTextStaffConfirmPassword);

        if (!validateForm(fullName, email, phone, password, confirmPassword)) {
            return;
        }

        viewModel.createStaff(fullName, email, phone, password, selectedHubId);
    }

    private boolean validateForm(String fullName,
                                 String email,
                                 String phone,
                                 String password,
                                 String confirmPassword) {
        clearErrors();
        boolean valid = true;

        if (fullName.isEmpty()) {
            binding.layoutStaffFullName.setError(getString(R.string.admin_add_staff_error_full_name));
            valid = false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutStaffEmail.setError(getString(R.string.admin_add_staff_error_email));
            valid = false;
        }
        if (phone.isEmpty()) {
            binding.layoutStaffPhone.setError(getString(R.string.admin_add_staff_error_phone));
            valid = false;
        }
        if (password.isEmpty()) {
            binding.layoutStaffPassword.setError(getString(R.string.admin_add_staff_error_password));
            valid = false;
        }
        if (confirmPassword.isEmpty() || !password.equals(confirmPassword)) {
            binding.layoutStaffConfirmPassword.setError(getString(R.string.admin_add_staff_error_confirm_password));
            valid = false;
        }
        if (!hubsLoaded || selectedHubId == null || selectedHubId.trim().isEmpty()) {
            binding.textAdminAddStaffHubError.setText(R.string.admin_add_staff_error_hub);
            binding.textAdminAddStaffHubError.setVisibility(View.VISIBLE);
            valid = false;
        }
        return valid;
    }

    private void clearErrors() {
        binding.layoutStaffFullName.setError(null);
        binding.layoutStaffEmail.setError(null);
        binding.layoutStaffPhone.setError(null);
        binding.layoutStaffPassword.setError(null);
        binding.layoutStaffConfirmPassword.setError(null);
        binding.textAdminAddStaffHubError.setVisibility(View.GONE);
    }

    private void handleCreateStaffSuccess(@Nullable Staff staff) {
        showToast(getString(R.string.admin_add_staff_success));
        popBackStackSafely();
    }

    private void applySubmitState(boolean isLoading) {
        binding.buttonCreateStaff.setEnabled(!isLoading && hubsLoaded);
        binding.buttonCreateStaff.setText(isLoading
                ? R.string.admin_add_staff_submitting
                : R.string.admin_add_staff_submit);
    }

    private String readText(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    @NonNull
    private String resolveHubName(@NonNull Hub hub) {
        String name = hub.getName();
        if (name == null || name.trim().isEmpty()) {
            return getString(R.string.admin_staff_hub_fallback);
        }
        return name.trim();
    }

    private static final class HubSpinnerItem {
        final String hubId;
        final String hubName;

        HubSpinnerItem(String hubId, String hubName) {
            this.hubId = hubId;
            this.hubName = hubName;
        }

        @NonNull
        @Override
        public String toString() {
            return hubName;
        }
    }
}
