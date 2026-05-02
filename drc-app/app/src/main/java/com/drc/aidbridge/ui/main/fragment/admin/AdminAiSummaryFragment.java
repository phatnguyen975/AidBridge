package com.drc.aidbridge.ui.main.fragment.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminAiSummaryBinding;
import com.drc.aidbridge.domain.model.admin.AdminDashboardSummary;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.admin.RecentActivitiesAdapter;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminAiSummaryViewModel;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminAiSummaryFragment extends BaseFragment<FragmentAdminAiSummaryBinding> {

    private AdminAiSummaryViewModel viewModel;
    private RecentActivitiesAdapter recentActivitiesAdapter;
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
    private final DecimalFormat growthFormat = new DecimalFormat("#0.##");

    @Nullable
    @Override
    protected FragmentAdminAiSummaryBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentAdminAiSummaryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AdminAiSummaryViewModel.class);
        setupRecyclerView();
        setupClickListeners();
        bindDefaultSummaryState();
    }

    private void setupClickListeners() {
        binding.btnGenerateReport.setOnClickListener(v -> {
            viewModel.generateReliefReport();
            showToast(getString(R.string.admin_ai_summary_toast_generate_report));
        });

        binding.textAdminAiViewAll
                .setOnClickListener(v -> showToast(getString(R.string.admin_ai_summary_toast_open_all_activities)));
    }

    private void setupRecyclerView() {
        recentActivitiesAdapter = new RecentActivitiesAdapter();
        binding.rvRecentActivities.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentActivities.setAdapter(recentActivitiesAdapter);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getDashboardSummary().observe(
                getViewLifecycleOwner(),
                resultObserver(this::bindSummary, this::showDashboardError)
        );

        viewModel.getGeneratingReport().observe(getViewLifecycleOwner(),
                isGenerating -> {
                    boolean generating = Boolean.TRUE.equals(isGenerating);
                    binding.btnGenerateReport.setEnabled(!generating);
                    binding.btnGenerateReport.setText(generating
                            ? getString(R.string.admin_ai_summary_generating)
                            : getString(R.string.admin_ai_summary_create_report));
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.loadDashboardSummary();
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        if (binding == null) {
            return;
        }
        binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.tvErrorMessage.setVisibility(View.GONE);
        binding.btnGenerateReport.setEnabled(!isLoading);
    }

    private void bindSummary(@Nullable AdminDashboardSummary summary) {
        if (binding == null || summary == null) {
            return;
        }

        binding.tvErrorMessage.setVisibility(View.GONE);
        binding.tvTotalPackages.setText(getString(
                R.string.admin_ai_summary_total_packages_format,
                formatCount(summary.getTotalPackages())
        ));
        binding.tvPackageGrowth.setText(getString(
                R.string.admin_ai_summary_growth_format,
                growthFormat.format(summary.getPackageGrowthPercent())
        ));
        binding.tvPeopleSupported.setText(getString(
                R.string.admin_ai_summary_people_supported_format,
                formatCount(summary.getTotalPeopleSupported())
        ));
        binding.textAdminAiLastUpdated.setText(getString(
                R.string.admin_ai_summary_updated_now_format,
                new SimpleDateFormat("HH:mm, dd/MM", Locale.forLanguageTag("vi-VN")).format(new Date())
        ));

        bindAlerts(summary.getAlerts());
        bindRecentActivities(summary.getRecentActivities());
    }

    private void bindAlerts(@Nullable List<AdminDashboardSummary.AdminAlert> alerts) {
        binding.layoutAlertsContainer.removeAllViews();

        if (alerts == null || alerts.isEmpty()) {
            binding.tvEmptyAlerts.setVisibility(View.VISIBLE);
            return;
        }

        binding.tvEmptyAlerts.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (AdminDashboardSummary.AdminAlert alert : alerts) {
            if (alert == null) {
                continue;
            }
            View view = inflater.inflate(R.layout.item_admin_alert, binding.layoutAlertsContainer, false);
            TextView title = view.findViewById(R.id.text_alert_title);
            TextView message = view.findViewById(R.id.text_alert_message);
            TextView severity = view.findViewById(R.id.text_alert_severity);
            ImageView icon = view.findViewById(R.id.image_alert_icon);

            title.setText(alert.getTitle().isEmpty()
                    ? getString(R.string.admin_ai_summary_alert_default_title)
                    : alert.getTitle());
            message.setText(alert.getMessage().isEmpty()
                    ? getString(R.string.admin_ai_summary_alert_default_message)
                    : alert.getMessage());

            severity.setText(resolveSeverityText(alert.getSeverity()));
            severity.setTextColor(ContextCompat.getColor(requireContext(), resolveSeverityColor(alert.getSeverity())));
            icon.setImageResource(resolveSeverityIcon(alert.getSeverity()));
            binding.layoutAlertsContainer.addView(view);
        }
    }

    private void bindRecentActivities(@Nullable List<AdminDashboardSummary.RecentActivity> recentActivities) {
        recentActivitiesAdapter.submitList(recentActivities);
        boolean isEmpty = recentActivities == null || recentActivities.isEmpty();
        binding.tvEmptyActivities.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void showDashboardError(@NonNull String message) {
        if (binding == null) {
            return;
        }
        binding.tvErrorMessage.setText(message);
        binding.tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void bindDefaultSummaryState() {
        binding.tvTotalPackages.setText(getString(R.string.admin_ai_summary_goods_placeholder));
        binding.tvPackageGrowth.setText(getString(R.string.admin_ai_summary_growth_placeholder));
        binding.tvPeopleSupported.setText(getString(R.string.admin_ai_summary_people_placeholder));
    }

    private String formatCount(long value) {
        return numberFormat.format(Math.max(0L, value));
    }

    private String resolveSeverityText(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            return getString(R.string.admin_alert_severity_critical);
        }
        if ("HIGH".equalsIgnoreCase(severity)) {
            return getString(R.string.admin_alert_severity_high);
        }
        if ("MEDIUM".equalsIgnoreCase(severity)) {
            return getString(R.string.admin_alert_severity_medium);
        }
        return getString(R.string.admin_alert_severity_low);
    }

    private int resolveSeverityIcon(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity) || "HIGH".equalsIgnoreCase(severity)) {
            return R.drawable.ic_admin_warning_overload;
        }
        return R.drawable.ic_admin_warning_water;
    }

    private int resolveSeverityColor(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            return R.color.admin_button_danger_bg;
        }
        if ("HIGH".equalsIgnoreCase(severity)) {
            return R.color.admin_ai_summary_warning_overload;
        }
        if ("MEDIUM".equalsIgnoreCase(severity)) {
            return R.color.admin_ai_summary_warning_water;
        }
        return R.color.admin_text_secondary;
    }
}