package com.drc.aidbridge.ui.main.fragment.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminDashboardBinding;
import com.drc.aidbridge.domain.model.admin.AdminDashboardSummary;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminDashboardViewModel;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminDashboardFragment extends BaseFragment<FragmentAdminDashboardBinding> {

    private static final String LOADING_PLACEHOLDER = "...";

    private AdminDashboardViewModel viewModel;
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));

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
        viewModel.getDashboardSummary().observe(
                getViewLifecycleOwner(),
                resultObserver(this::renderDashboardSummary, this::renderDashboardError)
        );
    }

    @Override
    public void onViewCreated(View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.loadDashboardSummary();
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        if (isLoading && binding != null) {
            setStatisticPlaceholders();
        }
    }

    private void setupClickListeners() {
        binding.buttonAdminOpenMap.setOnClickListener(v -> onOpenMapClicked());
        binding.buttonAdminManageHub.setOnClickListener(v -> onManageHubClicked());
    }

    private void onOpenMapClicked() {
        boolean navigated = navigateToDestinationSafely(R.id.adminMapFragment);
        if (!navigated) {
            showToast(getString(R.string.admin_dashboard_open_map_todo));
        }
    }

    private void onManageHubClicked() {
        boolean navigated = navigateToDestinationSafely(R.id.adminHubManagementFragment);
        if (!navigated) {
            showToast(getString(R.string.admin_dashboard_manage_hubs_todo));
        }
    }

    private void renderDashboardSummary(@Nullable AdminDashboardSummary summary) {
        if (summary == null) {
            showToast(getString(R.string.error_generic));
            return;
        }

        binding.textAdminTotalHubsValue.setText(formatCount(summary.getTotalHubs()));
        binding.textAdminVolunteersValue.setText(formatCount(summary.getTotalVolunteers()));
        binding.textAdminTodayMissionsValue.setText(formatCount(summary.getTodayMissions()));
        binding.textAdminDistributedItemsValue.setText(formatCount(summary.getDistributedItems()));
        renderInventoryChart(summary.getItemCategoryStats());
    }

    private void renderDashboardError(String message) {
        showToast(message);
    }

    private void setStatisticPlaceholders() {
        binding.textAdminTotalHubsValue.setText(LOADING_PLACEHOLDER);
        binding.textAdminVolunteersValue.setText(LOADING_PLACEHOLDER);
        binding.textAdminTodayMissionsValue.setText(LOADING_PLACEHOLDER);
        binding.textAdminDistributedItemsValue.setText(LOADING_PLACEHOLDER);
    }

    private String formatCount(long value) {
        return numberFormat.format(Math.max(0L, value));
    }

    private void renderInventoryChart(@Nullable List<AdminDashboardSummary.ItemCategoryStat> stats) {
        if (stats == null || stats.isEmpty()) {
            binding.barChartInventory.clear();
            binding.barChartInventory.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < stats.size(); i++) {
            AdminDashboardSummary.ItemCategoryStat stat = stats.get(i);
            long quantity = stat != null ? stat.getQuantity() : 0L;
            entries.add(new BarEntry(i, (float) Math.min(quantity, (long) Float.MAX_VALUE)));
            labels.add(safeCategoryLabel(stat, i));
        }

        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.admin_chart_title));
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.admin_action_button_primary_bg));
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        dataSet.setValueTextSize(10f);
        dataSet.setDrawValues(true);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.55f);

        binding.barChartInventory.setData(barData);
        binding.barChartInventory.getDescription().setEnabled(false);
        binding.barChartInventory.getLegend().setEnabled(false);
        binding.barChartInventory.setDrawGridBackground(false);
        binding.barChartInventory.setFitBars(true);

        XAxis xAxis = binding.barChartInventory.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labels.size(), false);
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(labels.size() - 0.5f);
        xAxis.setLabelRotationAngle(-25f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        YAxis yAxis = binding.barChartInventory.getAxisLeft();
        yAxis.setDrawGridLines(false);
        yAxis.setAxisMinimum(0f);
        yAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

        binding.barChartInventory.getAxisRight().setEnabled(false);
        binding.barChartInventory.animateY(450);
        binding.barChartInventory.invalidate();
    }

    private String safeCategoryLabel(@Nullable AdminDashboardSummary.ItemCategoryStat stat, int index) {
        if (stat != null && stat.getCategory() != null && !stat.getCategory().trim().isEmpty()) {
            return stat.getCategory().trim();
        }
        return String.valueOf(index + 1);
    }
}
