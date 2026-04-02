package com.drc.aidbridge.ui.main.fragment.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminAiSummaryBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminAiSummaryViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminAiSummaryFragment extends BaseFragment<FragmentAdminAiSummaryBinding> {

    private AdminAiSummaryViewModel viewModel;

    @Nullable
    @Override
    protected FragmentAdminAiSummaryBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentAdminAiSummaryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AdminAiSummaryViewModel.class);

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.buttonAdminAiCreateReport.setOnClickListener(v -> {
            viewModel.generateReliefReport();
            showToast(getString(R.string.admin_ai_summary_toast_generate_report));
        });

        binding.textAdminAiViewAll
                .setOnClickListener(v -> showToast(getString(R.string.admin_ai_summary_toast_open_all_activities)));

        binding.fabAdminAiRobot.setOnClickListener(v -> {
            boolean navigated = navigateToDestinationSafely(R.id.adminAiChatbotFragment);
            if (!navigated) {
                showToast(getString(R.string.admin_ai_summary_toast_open_chatbot_failed));
            }
        });
    }

    @Override
    protected void observeViewModel() {
        viewModel.getGeneratingReport().observe(getViewLifecycleOwner(),
                isGenerating -> binding.buttonAdminAiCreateReport.setEnabled(!Boolean.TRUE.equals(isGenerating)));
    }
}