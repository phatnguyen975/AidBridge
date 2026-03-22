package com.drc.aidbridge.ui.main.fragment.victim;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVictimSosSelfBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.victim.VictimSosPagerAdapter;
import com.google.android.material.tabs.TabLayoutMediator;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimSosSelfFragment extends BaseFragment<FragmentVictimSosSelfBinding> {

    @Nullable
    @Override
    protected FragmentVictimSosSelfBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimSosSelfBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        VictimSosPagerAdapter pagerAdapter = new VictimSosPagerAdapter(requireActivity());
        binding.viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2 and keep tab labels synchronized with page index
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(getTabTitle(position))
        ).attach();
    }

    @NonNull
    private String getTabTitle(int position) {
        if (position == 0) {
            return getString(R.string.victim_sos_tab_rescue);
        }
        return getString(R.string.victim_sos_tab_supply);
    }

    @Override
    protected void observeViewModel() {
        // TODO: Bind SOS request state once use-cases are integrated.
    }
}
