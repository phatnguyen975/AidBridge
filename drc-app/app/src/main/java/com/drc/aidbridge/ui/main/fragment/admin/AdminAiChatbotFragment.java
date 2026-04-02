package com.drc.aidbridge.ui.main.fragment.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentAdminAiChatbotBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.admin.AdminAiChatAdapter;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminAiChatbotViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminAiChatbotFragment extends BaseFragment<FragmentAdminAiChatbotBinding> {

    private AdminAiChatbotViewModel viewModel;
    private AdminAiChatAdapter chatAdapter;

    @Nullable
    @Override
    protected FragmentAdminAiChatbotBinding inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAdminAiChatbotBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(AdminAiChatbotViewModel.class);
        chatAdapter = new AdminAiChatAdapter(new AdminAiChatAdapter.ChatActionListener() {
            @Override
            public void onDispatchNow() {
                showToast(getString(R.string.admin_ai_chatbot_toast_dispatch_todo));
            }

            @Override
            public void onViewDetails() {
                showToast(getString(R.string.admin_ai_chatbot_toast_detail_todo));
            }
        });

        binding.recyclerAdminAiChatMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerAdminAiChatMessages.setAdapter(chatAdapter);
        binding.textAdminAiChatbotTitle.setText(getString(R.string.admin_ai_chatbot_header_title));
        binding.textAdminAiChatbotStatus.setText(getString(R.string.admin_ai_chatbot_header_subtitle));

        viewModel.loadChatHistory();
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.buttonAdminAiChatbotBack.setOnClickListener(v -> popBackStackSafely());

        binding.buttonAdminAiAttach
                .setOnClickListener(v -> showToast(getString(R.string.admin_ai_chatbot_toast_attach_todo)));
        binding.buttonAdminAiVoice
                .setOnClickListener(v -> showToast(getString(R.string.admin_ai_chatbot_toast_voice_todo)));

        binding.chipAdminAiSuggestInventory
                .setOnClickListener(v -> binding.editTextAdminAiMessage.setText(
                        getString(R.string.admin_ai_chatbot_chip_inventory)));
        binding.chipAdminAiSuggestMaintenance
                .setOnClickListener(v -> binding.editTextAdminAiMessage.setText(
                        getString(R.string.admin_ai_chatbot_chip_maintenance)));

        binding.buttonAdminAiSend.setOnClickListener(v -> {
            String message = binding.editTextAdminAiMessage.getText() == null
                    ? ""
                    : binding.editTextAdminAiMessage.getText().toString().trim();
            if (message.isEmpty()) {
                showToast(getString(R.string.admin_ai_chatbot_toast_empty_message));
                return;
            }

            viewModel.sendNewMessage(message);
            binding.editTextAdminAiMessage.setText("");
            showToast(getString(R.string.admin_ai_chatbot_toast_send));
        });
    }

    @Override
    protected void observeViewModel() {
        viewModel.getChatMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages == null) {
                return;
            }
            chatAdapter.submitList(messages);
            binding.recyclerAdminAiChatMessages.scrollToPosition(messages.size() - 1);
        });
    }
}
