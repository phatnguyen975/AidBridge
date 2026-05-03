package com.drc.aidbridge.ui.main.fragment.victim;

import android.view.LayoutInflater;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVictimSosRelativeBinding;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.map.victim.GoogleGeocodingClient;
import com.drc.aidbridge.ui.map.victim.RelativeLocationMapController;
import com.drc.aidbridge.ui.main.viewmodel.victim.VictimSosViewModel;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimSosRelativeFragment extends BaseFragment<FragmentVictimSosRelativeBinding> {

    private VictimSosViewModel viewModel;
    private RelativeLocationMapController relativeLocationMapController;
    private final GoogleGeocodingClient googleGeocodingClient = new GoogleGeocodingClient();
    private ExecutorService geocodingExecutor;

    private boolean isSubmitLoading;
    private boolean isAddressSearching;
    private boolean isUpdatingAddressFields;
    private boolean hasSearchPinnedLocation;
    private Double currentLatitude;
    private Double currentLongitude;

    @Nullable
    @Override
    protected FragmentVictimSosRelativeBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimSosRelativeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(VictimSosViewModel.class);
        geocodingExecutor = Executors.newSingleThreadExecutor();

        // TODO(test-cost): Hide map initialization after QA if this paid map flow is disabled.
        relativeLocationMapController = new RelativeLocationMapController(
            this,
            R.id.relative_location_map_container
        );
        relativeLocationMapController.initialize();

        String[] severityLevels = getResources().getStringArray(R.array.sos_severity_levels);
        if (severityLevels.length > 0) {
            binding.actRelativeSeverity.setText(severityLevels[0], false);
        }

        binding.etRelativeLocationSearch.setOnEditorActionListener(this::handleSearchEditorAction);
        binding.btnSearchRelativeLocation.setOnClickListener(v -> searchRelativeAddress());

        binding.btnSubmitRelativeSos.setOnClickListener(v -> extractDataAndSubmit());

        binding.etRelativeAddress.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdatingAddressFields) {
                    return;
                }
                invalidatePinnedLocation();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        binding.etRelativeLocationSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdatingAddressFields) {
                    return;
                }
                invalidatePinnedLocation();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
    }

    @Override
    protected void observeViewModel() {
        viewModel.getValidationError().observe(getViewLifecycleOwner(), this::renderValidationError);

        viewModel.getSubmitRelativeSosResult().observe(
            getViewLifecycleOwner(),
            resultObserver(this::handleSubmitSuccess, this::handleSubmitError)
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        if (relativeLocationMapController != null) {
            relativeLocationMapController.onResume();
        }
    }

    @Override
    public void onPause() {
        if (relativeLocationMapController != null) {
            relativeLocationMapController.onPause();
        }
        super.onPause();
    }

    private void renderValidationError(@Nullable ValidationResult validation) {
        if (validation == null || validation.isValid()) {
            return;
        }

        String message = validation.getErrorMessage();
        showTopSnackbar(
            binding.getRoot(),
            message != null && !message.trim().isEmpty()
                ? message
                : getString(R.string.victim_relative_sos_submit_error),
            true
        );
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        isSubmitLoading = isLoading;
        boolean enabled = !isSubmitLoading && !isAddressSearching;
        binding.btnSubmitRelativeSos.setEnabled(enabled);
        binding.btnSubmitRelativeSos.setAlpha(enabled ? 1f : 0.7f);
    }

    private void extractDataAndSubmit() {
        clearInputFocusAndHideKeyboard();
        RelativeSosFormInput rawInput = collectRawInput();
        if (rawInput == null) {
            return;
        }

        if (!hasSearchPinnedLocation || currentLatitude == null || currentLongitude == null) {
            showTopSnackbar(
                binding.getRoot(),
                getString(R.string.victim_relative_sos_location_required),
                true
            );
            return;
        }

        submitRelativeSos(rawInput, currentLatitude, currentLongitude);
    }

    @Nullable
    private RelativeSosFormInput collectRawInput() {
        String name = getRawText(binding.etRelativeName).trim();
        String address = getRawText(binding.etRelativeAddress).trim();
        String phone = getRawText(binding.etRelativePhone).trim();
        String severity = getRawText(binding.actRelativeSeverity).trim();

        if (severity.isEmpty()) {
            severity = getDefaultSeverity();
        }

        return new RelativeSosFormInput(name, address, phone, severity);
    }

    private void handleSubmitSuccess(@Nullable String message) {
        String successMessage = (message != null && !message.trim().isEmpty())
            ? message
            : getString(R.string.victim_relative_sos_submit_success);
        showTopSnackbar(binding.getRoot(), successMessage, false);
        clearFormAfterSubmit();
    }

    private void handleSubmitError(String message) {
        String errorMessage = (message != null && !message.trim().isEmpty())
            ? message
            : getString(R.string.victim_relative_sos_submit_error);
        showTopSnackbar(binding.getRoot(), errorMessage, true);
    }

    private void clearFormAfterSubmit() {
        binding.etRelativeName.setText(null);
        binding.etRelativeAddress.setText(null);
        binding.etRelativeLocationSearch.setText(null);
        binding.etRelativePhone.setText(null);

        String defaultSeverity = getDefaultSeverity();
        if (!defaultSeverity.isEmpty()) {
            binding.actRelativeSeverity.setText(defaultSeverity, false);
        }

        invalidatePinnedLocation();
        if (relativeLocationMapController != null) {
            relativeLocationMapController.clearPin();
        }
    }

    private boolean handleSearchEditorAction(TextView textView, int actionId, KeyEvent event) {
        boolean isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH
            || actionId == EditorInfo.IME_ACTION_DONE;
        if (!isSearchAction && event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            isSearchAction = true;
        }

        if (isSearchAction) {
            searchRelativeAddress();
            return true;
        }
        return false;
    }

    private void searchRelativeAddress() {
        if (isAddressSearching) {
            return;
        }

        clearInputFocusAndHideKeyboard();

        String query = getRawText(binding.etRelativeLocationSearch).trim();
        if (query.isEmpty()) {
            query = getRawText(binding.etRelativeAddress).trim();
        }
        if (query.isEmpty()) {
            showTopSnackbar(
                binding.getRoot(),
                getString(R.string.victim_relative_sos_search_required),
                true
            );
            return;
        }
        final String searchQuery = query;
        final android.content.Context appContext = requireContext().getApplicationContext();

        setAddressSearching(true);

        // TODO(test-cost): Disable this geocoding lookup after QA to avoid paid map request charges.
        geocodingExecutor.execute(() -> {
            try {
                GoogleGeocodingClient.GeocodingResult geocodingResult =
                    googleGeocodingClient.geocodeFirstAddress(appContext, searchQuery);

                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    setAddressSearching(false);

                    if (geocodingResult == null) {
                        showTopSnackbar(
                            binding.getRoot(),
                            getString(R.string.victim_relative_sos_search_not_found),
                            true
                        );
                        return;
                    }

                    currentLatitude = geocodingResult.latitude;
                    currentLongitude = geocodingResult.longitude;
                    hasSearchPinnedLocation = true;

                    String resolvedAddress = geocodingResult.formattedAddress;
                    if (!resolvedAddress.isEmpty()) {
                        isUpdatingAddressFields = true;
                        binding.etRelativeAddress.setText(resolvedAddress);
                        binding.etRelativeLocationSearch.setText(resolvedAddress);
                        isUpdatingAddressFields = false;
                    }

                    if (relativeLocationMapController != null) {
                        relativeLocationMapController.pinLocation(
                            geocodingResult.latitude,
                            geocodingResult.longitude,
                            getString(R.string.victim_relative_sos_pin_title)
                        );
                    }
                });
            } catch (IOException exception) {
                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    setAddressSearching(false);
                    showTopSnackbar(
                        binding.getRoot(),
                        getString(R.string.victim_relative_sos_search_failed),
                        true
                    );
                });
            }
        });
    }

    private void setAddressSearching(boolean searching) {
        isAddressSearching = searching;
        if (binding == null) {
            return;
        }

        binding.btnSearchRelativeLocation.setEnabled(!searching);
        binding.btnSearchRelativeLocation.setAlpha(searching ? 0.7f : 1f);

        boolean submitEnabled = !isSubmitLoading && !isAddressSearching;
        binding.btnSubmitRelativeSos.setEnabled(submitEnabled);
        binding.btnSubmitRelativeSos.setAlpha(submitEnabled ? 1f : 0.7f);
    }

    private void invalidatePinnedLocation() {
        currentLatitude = null;
        currentLongitude = null;
        hasSearchPinnedLocation = false;
        if (relativeLocationMapController != null) {
            relativeLocationMapController.clearPin();
        }
    }

    @Override
    public void onDestroyView() {
        if (relativeLocationMapController != null) {
            relativeLocationMapController.onDestroy();
            relativeLocationMapController = null;
        }
        if (geocodingExecutor != null) {
            geocodingExecutor.shutdownNow();
            geocodingExecutor = null;
        }
        super.onDestroyView();
    }

    private void submitRelativeSos(RelativeSosFormInput input, double latitude, double longitude) {
        viewModel.submitRelativeSos(
            input.name,
            input.phone,
            input.address,
            input.severity,
            latitude,
            longitude
        );
    }

    private String getRawText(EditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString();
    }

    private String getDefaultSeverity() {
        String[] levels = getResources().getStringArray(R.array.sos_severity_levels);
        if (levels.length == 0) {
            return "";
        }
        return levels[0];
    }

    private static final class RelativeSosFormInput {
        final String name;
        final String address;
        final String phone;
        final String severity;

        RelativeSosFormInput(String name, String address, String phone, String severity) {
            this.name = name;
            this.address = address;
            this.phone = phone;
            this.severity = severity;
        }
    }
}
