package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentSponsorDashboardBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.sponsor.SponsorRecentAdapter;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorDashboardFragment extends BaseFragment<FragmentSponsorDashboardBinding> {

    private SponsorRecentAdapter sponsorRecentAdapter;

    @Nullable
    @Override
    protected FragmentSponsorDashboardBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorDashboardBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        binding.cardRegisterDonation.setOnClickListener(v -> navigateToDestinationSafely(R.id.sponsorDonateFragment));
        binding.cardDonationHistory.setOnClickListener(v -> navigateToDestinationSafely(R.id.sponsorHistoryFragment));

        sponsorRecentAdapter = new SponsorRecentAdapter(item -> navigateToDestinationSafely(R.id.sponsorHistoryFragment));
        binding.rvRecentDonations.setAdapter(sponsorRecentAdapter);

        loadMockRecentData();
    }

    @Override
    protected void observeViewModel() {
    }

    /**
     * Loads temporary dashboard items for UI development in Phase 2.
     */
    private void loadMockRecentData() {
        // TODO: Replace this mock method with ViewModel LiveData observation
        sponsorRecentAdapter.submitItems(buildMockRecentData());
    }

    private List<SponsorRecentAdapter.SponsorRecentItem> buildMockRecentData() {
        List<SponsorRecentAdapter.SponsorRecentItem> items = new ArrayList<>();

        items.add(new SponsorRecentAdapter.SponsorRecentItem(
                getString(R.string.sponsor_recent_date_1),
                getString(R.string.sponsor_recent_category_food),
                getString(R.string.sponsor_recent_quantity_food),
                getString(R.string.sponsor_recent_status_stocked),
                ContextCompat.getColor(requireContext(), R.color.hub_blue),
            ContextCompat.getColor(requireContext(), R.color.hub_blue),
            R.mipmap.ic_launcher
        ));

        items.add(new SponsorRecentAdapter.SponsorRecentItem(
                getString(R.string.sponsor_recent_date_2),
                getString(R.string.sponsor_recent_category_clothes),
                getString(R.string.sponsor_recent_quantity_clothes),
                getString(R.string.sponsor_recent_status_shipping),
                ContextCompat.getColor(requireContext(), R.color.warning_orange),
            ContextCompat.getColor(requireContext(), R.color.warning_orange),
            R.mipmap.ic_launcher
        ));

        return items;
    }
}
