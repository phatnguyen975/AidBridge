package com.drc.aidbridge.ui.main.fragment.admin;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminAddHubBinding;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.map.victim.GoogleGeocodingClient;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminAddHubViewModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminAddHubFragment extends BaseFragment<FragmentAdminAddHubBinding> {

    private static final int ADDRESS_SUGGESTION_MIN_LENGTH = 3;
    private static final long ADDRESS_SUGGESTION_DEBOUNCE_MS = 450L;
    private static final int ADDRESS_SUGGESTION_LIMIT = 5;

    private AdminAddHubViewModel viewModel;
    private final GoogleGeocodingClient googleGeocodingClient = new GoogleGeocodingClient();
    private final Handler addressSuggestionHandler = new Handler(Looper.getMainLooper());
    private final List<GoogleGeocodingClient.GeocodingResult> addressSuggestions = new ArrayList<>();
    private ExecutorService geocodingExecutor;
    private ArrayAdapter<String> addressSuggestionAdapter;
    private GoogleGeocodingClient.GeocodingResult selectedAddressSuggestion;
    private boolean isGeocoding;
    private boolean isCreateLoading;
    private boolean suppressAddressWatcher;
    private int addressSuggestionRequestVersion;
    private String startTime;
    private String endTime;
    private int startMinutes = -1;
    private int endMinutes = -1;

    @Nullable
    @Override
    protected FragmentAdminAddHubBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAdminAddHubBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AdminAddHubViewModel.class);
        geocodingExecutor = Executors.newSingleThreadExecutor();
        setupAddressSuggestions();
        binding.editTextAdminAddHubStartTime.setOnClickListener(v -> showTimePicker(true));
        binding.editTextAdminAddHubEndTime.setOnClickListener(v -> showTimePicker(false));
        binding.buttonAdminAddHubBack.setOnClickListener(v -> popBackStackSafely());
        binding.buttonAdminAddHubSubmit.setOnClickListener(v -> submitHub());
    }

    private void setupAddressSuggestions() {
        addressSuggestionAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>());
        binding.editTextAdminAddHubAddress.setAdapter(addressSuggestionAdapter);
        binding.editTextAdminAddHubAddress.setThreshold(ADDRESS_SUGGESTION_MIN_LENGTH);
        binding.editTextAdminAddHubAddress.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= addressSuggestions.size()) {
                return;
            }

            selectedAddressSuggestion = addressSuggestions.get(position);
            addressSuggestionRequestVersion++;
            addressSuggestionHandler.removeCallbacksAndMessages(null);
            String displayAddress = displayAddress(selectedAddressSuggestion);
            suppressAddressWatcher = true;
            binding.editTextAdminAddHubAddress.setText(displayAddress, false);
            binding.editTextAdminAddHubAddress.setSelection(displayAddress.length());
            suppressAddressWatcher = false;
            binding.editTextAdminAddHubAddress.dismissDropDown();
            binding.layoutAdminAddHubAddress.setError(null);
        });
        binding.editTextAdminAddHubAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (suppressAddressWatcher) {
                    return;
                }
                selectedAddressSuggestion = null;
                scheduleAddressSuggestions(text(s));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    protected void observeViewModel() {
        viewModel.getCreateHubResult().observe(getViewLifecycleOwner(),
                resultObserver(this::handleCreateHubSuccess, this::showCreateHubError));
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        isCreateLoading = isLoading;
        updateSubmitLoadingState();
    }

    private void updateSubmitLoadingState() {
        if (binding == null) {
            return;
        }

        boolean loading = isGeocoding || isCreateLoading;
        binding.progressAdminAddHub.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.buttonAdminAddHubSubmit.setEnabled(!loading);
        binding.buttonAdminAddHubSubmit.setText(isGeocoding
                ? R.string.admin_add_hub_geocoding
                : isCreateLoading
                        ? R.string.admin_add_hub_submitting
                        : R.string.admin_add_hub_submit);
    }

    private void submitHub() {
        clearInputFocusAndHideKeyboard();
        clearErrors();

        String name = text(binding.editTextAdminAddHubName.getText());
        String address = text(binding.editTextAdminAddHubAddress.getText());
        String phone = text(binding.editTextAdminAddHubPhone.getText());
        String imageUrl = text(binding.editTextAdminAddHubImageUrl.getText());

        boolean valid = true;
        if (name.isEmpty()) {
            binding.layoutAdminAddHubName.setError(getString(R.string.admin_add_hub_error_name));
            valid = false;
        }
        if (address.isEmpty()) {
            binding.layoutAdminAddHubAddress.setError(getString(R.string.admin_add_hub_error_address));
            valid = false;
        }
        if (phone.isEmpty()) {
            binding.layoutAdminAddHubPhone.setError(getString(R.string.admin_add_hub_error_phone));
            valid = false;
        }
        if (startTime == null || startTime.trim().isEmpty()) {
            binding.layoutAdminAddHubStartTime.setError(getString(R.string.admin_add_hub_error_start_time));
            valid = false;
        }
        if (endTime == null || endTime.trim().isEmpty()) {
            binding.layoutAdminAddHubEndTime.setError(getString(R.string.admin_add_hub_error_end_time));
            valid = false;
        } else if (startMinutes >= 0 && endMinutes <= startMinutes) {
            binding.layoutAdminAddHubEndTime.setError(getString(R.string.admin_add_hub_error_time_order));
            valid = false;
        }

        if (!valid) {
            return;
        }

        String operatingHours = startTime + " - " + endTime;
        geocodeAddressAndCreateHub(name, address, phone, imageUrl, operatingHours);
    }

    private void geocodeAddressAndCreateHub(@NonNull String name,
            @NonNull String address,
            @NonNull String phone,
            @NonNull String imageUrl,
            @NonNull String operatingHours) {
        Context appContext = requireContext().getApplicationContext();
        GoogleGeocodingClient.GeocodingResult knownSuggestion = selectedAddressSuggestion;
        if (knownSuggestion != null && address.equals(displayAddress(knownSuggestion))) {
            viewModel.createHub(
                    name,
                    address,
                    phone,
                    imageUrl,
                    operatingHours,
                    knownSuggestion.latitude,
                    knownSuggestion.longitude);
            return;
        }

        if (geocodingExecutor == null || geocodingExecutor.isShutdown()) {
            geocodingExecutor = Executors.newSingleThreadExecutor();
        }

        setGeocoding(true);
        geocodingExecutor.execute(() -> {
            try {
                GoogleGeocodingClient.GeocodingResult geocodingResult = googleGeocodingClient
                        .geocodeFirstAddress(appContext, address);

                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    setGeocoding(false);
                    if (binding == null) {
                        return;
                    }

                    if (geocodingResult == null) {
                        binding.layoutAdminAddHubAddress.setError(
                                getString(R.string.admin_add_hub_error_geocode_not_found));
                        showToast(getString(R.string.admin_add_hub_error_geocode_not_found));
                        return;
                    }

                    viewModel.createHub(
                            name,
                            address,
                            phone,
                            imageUrl,
                            operatingHours,
                            geocodingResult.latitude,
                            geocodingResult.longitude);
                });
            } catch (IOException exception) {
                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    setGeocoding(false);
                    showToast(getString(R.string.admin_add_hub_error_geocode_failed));
                });
            }
        });
    }

    private void setGeocoding(boolean geocoding) {
        isGeocoding = geocoding;
        updateSubmitLoadingState();
    }

    private void handleCreateHubSuccess(@Nullable Hub hub) {
        showToast(getString(R.string.admin_add_hub_success));
        popBackStackSafely(R.id.adminHubManagementFragment, false);
    }

    private void showCreateHubError(@NonNull String message) {
        showToast(message.trim().isEmpty()
                ? getString(R.string.admin_add_hub_error_generic)
                : message);
    }

    private void clearErrors() {
        binding.layoutAdminAddHubName.setError(null);
        binding.layoutAdminAddHubAddress.setError(null);
        binding.layoutAdminAddHubPhone.setError(null);
        binding.layoutAdminAddHubStartTime.setError(null);
        binding.layoutAdminAddHubEndTime.setError(null);
    }

    @NonNull
    private String text(@Nullable CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void showTimePicker(boolean start) {
        int defaultHour = start ? 8 : 22;
        int defaultMinute = 0;
        int currentMinutes = start ? startMinutes : endMinutes;
        if (currentMinutes >= 0) {
            defaultHour = currentMinutes / 60;
            defaultMinute = currentMinutes % 60;
        }

        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    String formatted = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    int minutes = hourOfDay * 60 + minute;
                    if (start) {
                        startTime = formatted;
                        startMinutes = minutes;
                        binding.editTextAdminAddHubStartTime.setText(formatted);
                        binding.layoutAdminAddHubStartTime.setError(null);
                    } else {
                        endTime = formatted;
                        endMinutes = minutes;
                        binding.editTextAdminAddHubEndTime.setText(formatted);
                        binding.layoutAdminAddHubEndTime.setError(null);
                    }
                },
                defaultHour,
                defaultMinute,
                true);
        dialog.show();
    }

    private void scheduleAddressSuggestions(@NonNull String query) {
        addressSuggestionHandler.removeCallbacksAndMessages(null);
        int requestVersion = ++addressSuggestionRequestVersion;
        if (query.length() < ADDRESS_SUGGESTION_MIN_LENGTH) {
            updateAddressSuggestionDropdown(new ArrayList<>());
            return;
        }

        addressSuggestionHandler.postDelayed(
                () -> loadAddressSuggestions(query, requestVersion),
                ADDRESS_SUGGESTION_DEBOUNCE_MS);
    }

    private void loadAddressSuggestions(@NonNull String query, int requestVersion) {
        Context appContext = requireContext().getApplicationContext();
        if (geocodingExecutor == null || geocodingExecutor.isShutdown()) {
            geocodingExecutor = Executors.newSingleThreadExecutor();
        }

        geocodingExecutor.execute(() -> {
            try {
                List<GoogleGeocodingClient.GeocodingResult> suggestions = googleGeocodingClient
                        .geocodeAddressSuggestions(
                                appContext,
                                query,
                                ADDRESS_SUGGESTION_LIMIT);
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (requestVersion == addressSuggestionRequestVersion) {
                        updateAddressSuggestionDropdown(suggestions);
                    }
                });
            } catch (IOException ignored) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (requestVersion == addressSuggestionRequestVersion) {
                        updateAddressSuggestionDropdown(new ArrayList<>());
                    }
                });
            }
        });
    }

    private void updateAddressSuggestionDropdown(
            @NonNull List<GoogleGeocodingClient.GeocodingResult> suggestions) {
        if (binding == null || addressSuggestionAdapter == null) {
            return;
        }

        addressSuggestions.clear();
        addressSuggestions.addAll(suggestions);
        addressSuggestionAdapter.clear();
        for (GoogleGeocodingClient.GeocodingResult suggestion : suggestions) {
            String display = displayAddress(suggestion);
            if (!display.isEmpty()) {
                addressSuggestionAdapter.add(display);
            }
        }
        addressSuggestionAdapter.notifyDataSetChanged();

        if (!suggestions.isEmpty() && binding.editTextAdminAddHubAddress.hasFocus()) {
            binding.editTextAdminAddHubAddress.showDropDown();
        } else {
            binding.editTextAdminAddHubAddress.dismissDropDown();
        }
    }

    @NonNull
    private String displayAddress(@Nullable GoogleGeocodingClient.GeocodingResult suggestion) {
        if (suggestion == null) {
            return "";
        }
        String formatted = suggestion.formattedAddress != null ? suggestion.formattedAddress.trim() : "";
        if (!formatted.isEmpty()) {
            return formatted;
        }
        return String.format(Locale.US, "%.6f, %.6f", suggestion.latitude, suggestion.longitude);
    }

    @Override
    public void onDestroyView() {
        addressSuggestionHandler.removeCallbacksAndMessages(null);
        if (geocodingExecutor != null) {
            geocodingExecutor.shutdownNow();
            geocodingExecutor = null;
        }
        super.onDestroyView();
    }
}
