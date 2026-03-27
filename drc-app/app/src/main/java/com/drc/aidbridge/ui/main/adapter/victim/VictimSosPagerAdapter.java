package com.drc.aidbridge.ui.main.adapter.victim;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.drc.aidbridge.ui.main.fragment.victim.VictimRescueTabFragment;
import com.drc.aidbridge.ui.main.fragment.victim.VictimSupplyTabFragment;

/**
 * Pager adapter for the Victim SOS container tabs.
 */
public class VictimSosPagerAdapter extends FragmentStateAdapter {

    public VictimSosPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new VictimRescueTabFragment();
        }
        return new VictimSupplyTabFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
