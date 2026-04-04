package com.drc.aidbridge.ui.main.fragment.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminHubDetailBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.admin.AdminHubDetailInventoryAdapter;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminHubDetailViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminHubDetailFragment extends BaseFragment<FragmentAdminHubDetailBinding> {

    private AdminHubDetailViewModel viewModel;
    private AdminHubDetailInventoryAdapter inventoryAdapter;

    @Nullable
    @Override
    protected FragmentAdminHubDetailBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAdminHubDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AdminHubDetailViewModel.class);
        inventoryAdapter = new AdminHubDetailInventoryAdapter();

        binding.recyclerAdminHubInventory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerAdminHubInventory.setAdapter(inventoryAdapter);

        setupClickListeners();
        viewModel.loadMockInventory();
    }

    private void setupClickListeners() {
        binding.buttonAdminHubDetailBack.setOnClickListener(v -> popBackStackSafely());
        binding.buttonAddInventory
                .setOnClickListener(v -> navigateToDestinationSafely(R.id.adminAddSupplyTypeFragment));
    }

    @Override
    protected void observeViewModel() {
        viewModel.getInventoryCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                inventoryAdapter.submitList(categories);
            }
        });
    }
}
