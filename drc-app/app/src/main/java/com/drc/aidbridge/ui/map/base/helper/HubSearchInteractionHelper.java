package com.drc.aidbridge.ui.map.base.helper;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.dto.response.hub.HubDto;
import com.drc.aidbridge.databinding.FragmentMapBaseBinding;
import com.drc.aidbridge.ui.map.feature.hub.HubSearchDrawerFragment;

public class HubSearchInteractionHelper {

    public interface HubSearchListener {
        void onHubSelected(@NonNull HubDto hub);
    }

    public void setup(@NonNull Fragment fragment,
                      @NonNull FragmentMapBaseBinding binding,
                      @NonNull com.drc.aidbridge.ui.map.base.BaseMapViewModel viewModel,
                      @NonNull HubSearchListener listener) {
        
        FragmentManager fm = fragment.getChildFragmentManager();
        HubSearchDrawerFragment drawerFragment = 
            (HubSearchDrawerFragment) fm.findFragmentById(R.id.hubSearchFragmentContainer);
            
        if (drawerFragment != null) {
            drawerFragment.setViewModel(viewModel);
            drawerFragment.setListener(listener::onHubSelected);
        }

        binding.fabFindHubs.setOnClickListener(v -> openDrawer(binding));
    }

    public void openDrawer(@NonNull FragmentMapBaseBinding binding) {
        if (binding.drawerLayout != null) {
            binding.drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public void closeDrawer(@NonNull FragmentMapBaseBinding binding) {
        if (binding.drawerLayout != null) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
}
