package com.drc.aidbridge.ui.main.fragment.admin;

import android.text.Editable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminAddSupplyTypeBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminAddSupplyTypeViewModel;

import java.util.Arrays;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminAddSupplyTypeFragment extends BaseFragment<FragmentAdminAddSupplyTypeBinding> {

    private AdminAddSupplyTypeViewModel viewModel;
    private String[] categoryOptions;
    private String[] unitOptions;

    @Nullable
    @Override
    protected FragmentAdminAddSupplyTypeBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAdminAddSupplyTypeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AdminAddSupplyTypeViewModel.class);
        categoryOptions = getResources().getStringArray(R.array.admin_add_supply_type_categories);
        unitOptions = getResources().getStringArray(R.array.admin_add_supply_type_units);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_admin_spinner_selected,
                categoryOptions);
        categoryAdapter.setDropDownViewResource(R.layout.item_admin_spinner_dropdown);
        binding.spinnerSupplyCategory.setAdapter(categoryAdapter);

        viewModel.setExistingSupplyTypes(
                Arrays.asList(getResources().getStringArray(R.array.admin_add_supply_type_existing_mock)));
        updateUnitField(binding.spinnerSupplyCategory.getSelectedItemPosition());

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.buttonAdminAddSupplyBack.setOnClickListener(v -> popBackStackSafely());

        binding.spinnerSupplyCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                updateUnitField(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateUnitField(0);
            }
        });

        binding.buttonSaveSupplyType.setOnClickListener(v -> onSaveSupplyTypeClicked());
    }

    @Override
    protected void observeViewModel() {
        // TODO: Observe upsert result state from backend when UseCase is integrated.
    }

    private void updateUnitField(int position) {
        if (unitOptions.length == 0) {
            binding.editTextSupplyUnit.setText("");
            return;
        }
        int safePosition = Math.max(0, Math.min(position, unitOptions.length - 1));
        binding.editTextSupplyUnit.setText(unitOptions[safePosition]);
    }

    private void onSaveSupplyTypeClicked() {
        if (categoryOptions.length == 0) {
            return;
        }
        int selectedPosition = binding.spinnerSupplyCategory.getSelectedItemPosition();
        int safePosition = Math.max(0, Math.min(selectedPosition, categoryOptions.length - 1));
        String category = categoryOptions[safePosition];

        String minLevelRaw = getTextValue(binding.editTextSupplyMinLevel.getText());
        Integer minLevel = parsePositiveNumber(minLevelRaw);
        if (minLevel == null) {
            showToast(getString(R.string.admin_add_supply_type_toast_invalid_min));
            return;
        }

        String note = getTextValue(binding.editTextSupplyNote.getText());
        boolean isUpdated = viewModel.upsertSupplyType(category, minLevel, note);

        if (isUpdated) {
            showToast(getString(R.string.admin_add_supply_type_toast_updated, category));
        } else {
            showToast(getString(R.string.admin_add_supply_type_toast_added, category));
        }
        popBackStackSafely();
    }

    @NonNull
    private String getTextValue(@Nullable Editable editable) {
        return editable == null ? "" : editable.toString().trim();
    }

    @Nullable
    private Integer parsePositiveNumber(@NonNull String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
