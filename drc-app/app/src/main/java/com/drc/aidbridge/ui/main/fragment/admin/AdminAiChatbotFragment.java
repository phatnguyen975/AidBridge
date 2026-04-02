package com.drc.aidbridge.ui.main.fragment.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminAiChatbotBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminAiChatbotFragment extends BaseFragment<FragmentAdminAiChatbotBinding> {

    @Nullable
    @Override
    protected FragmentAdminAiChatbotBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAdminAiChatbotBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        binding.textAdminAiChatbotTitle.setText(getString(R.string.admin_ai_chatbot_title));
    }

    @Override
    protected void observeViewModel() {
    }
}
