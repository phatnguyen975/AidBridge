package com.drc.aidbridge.ui.main.fragment.admin;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminHubManagementBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.admin.AdminHubAdapter;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminHubManagementViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminHubManagementFragment extends BaseFragment<FragmentAdminHubManagementBinding>
        implements AdminHubAdapter.HubActionListener {

    private AdminHubManagementViewModel viewModel;
    private AdminHubAdapter hubAdapter;
    private final List<AdminHubManagementViewModel.Hub> allHubs = new ArrayList<>();

    @Nullable
    @Override
    protected FragmentAdminHubManagementBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAdminHubManagementBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AdminHubManagementViewModel.class);
        hubAdapter = new AdminHubAdapter(this);

        binding.recyclerAdminHubs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerAdminHubs.setAdapter(hubAdapter);

        binding.buttonAdminHubBack.setOnClickListener(v -> popBackStackSafely());
        binding.fabAdminAddHub.setOnClickListener(v -> showToast(getString(R.string.admin_hub_mgmt_toast_add_new)));

        binding.editTextAdminHubSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        viewModel.loadMockHubs();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getHubs().observe(getViewLifecycleOwner(), hubs -> {
            allHubs.clear();
            if (hubs != null) {
                allHubs.addAll(hubs);
            }
            applyFilter(getSearchQuery());
        });
    }

    @Override
    public void onToggleHubStatus(@NonNull AdminHubManagementViewModel.Hub hub) {
        viewModel.toggleHubStatus(hub.id);
        String hubName = getString(hub.nameResId);
        int statusRes = hub.isActive ? R.string.admin_hub_mgmt_status_suspended : R.string.admin_hub_mgmt_status_active;
        showToast(getString(R.string.admin_hub_mgmt_toast_status_changed, hubName, getString(statusRes)));
    }

    private void applyFilter(@NonNull String query) {
        String normalizedQuery = query.trim().toLowerCase(Locale.getDefault());
        if (normalizedQuery.isEmpty()) {
            hubAdapter.submitList(new ArrayList<>(allHubs));
            return;
        }

        List<AdminHubManagementViewModel.Hub> filtered = new ArrayList<>();
        for (AdminHubManagementViewModel.Hub hub : allHubs) {
            String name = getString(hub.nameResId).toLowerCase(Locale.getDefault());
            String address = getString(hub.addressResId).toLowerCase(Locale.getDefault());
            if (name.contains(normalizedQuery) || address.contains(normalizedQuery)) {
                filtered.add(hub);
            }
        }
        hubAdapter.submitList(filtered);
    }

    @NonNull
    private String getSearchQuery() {
        Editable text = binding.editTextAdminHubSearch.getText();
        return text != null ? text.toString() : "";
    }
}