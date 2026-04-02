package com.drc.aidbridge.ui.main.fragment.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminDashboardBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminDashboardViewModel;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminDashboardFragment extends BaseFragment<FragmentAdminDashboardBinding> {

    private AdminDashboardViewModel viewModel;

    @Nullable
    @Override
    protected FragmentAdminDashboardBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentAdminDashboardBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AdminDashboardViewModel.class);
        binding.textAdminDashboardTitle.setText(getString(R.string.admin_dashboard_title));
        setupClickListeners();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getInventoryData().observe(getViewLifecycleOwner(),
                resultObserver(null, this::renderInventoryChart));
    }

    @Override
    public void onViewCreated(View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.loadInventoryData();
    }

    private void setupClickListeners() {
        binding.buttonAdminOpenMap.setOnClickListener(v -> onOpenMapClicked());
        binding.buttonAdminManageHub.setOnClickListener(v -> onManageHubClicked());
    }

    private void onOpenMapClicked() {
        showToast(getString(R.string.admin_dashboard_open_map_todo));
    }

    private void onManageHubClicked() {
        boolean navigated = navigateToDestinationSafely(R.id.adminHubManagementFragment);
        if (!navigated) {
            showToast(getString(R.string.admin_dashboard_manage_hubs_todo));
        }
    }

    private void renderInventoryChart(@Nullable float[] values) {
        if (values == null || values.length == 0) {
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            entries.add(new BarEntry(i, values[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.admin_chart_title));
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.admin_action_button_primary_bg));
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        dataSet.setDrawValues(true);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.55f);

        binding.barChartInventory.setData(barData);
        binding.barChartInventory.getDescription().setEnabled(false);
        binding.barChartInventory.getLegend().setEnabled(true);
        binding.barChartInventory.getLegend()
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        binding.barChartInventory.setDrawGridBackground(false);

        XAxis xAxis = binding.barChartInventory.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(4);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[] {
                getString(R.string.admin_chart_label_medical),
                getString(R.string.admin_chart_label_food),
                getString(R.string.admin_chart_label_water),
                getString(R.string.admin_chart_label_clothes)
        }));

        YAxis yAxis = binding.barChartInventory.getAxisLeft();
        yAxis.setDrawGridLines(false);
        yAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        binding.barChartInventory.getAxisRight().setEnabled(false);
        binding.barChartInventory.animateY(450);
        binding.barChartInventory.invalidate();
    }
}
