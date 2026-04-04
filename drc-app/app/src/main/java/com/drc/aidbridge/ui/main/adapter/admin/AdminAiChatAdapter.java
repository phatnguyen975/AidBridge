package com.drc.aidbridge.ui.main.adapter.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.databinding.ItemAdminChatMessageAiBinding;
import com.drc.aidbridge.databinding.ItemAdminChatMessageUserBinding;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminAiChatbotViewModel;

import java.util.ArrayList;
import java.util.List;

public class AdminAiChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ADMIN = 0;
    private static final int VIEW_TYPE_AI = 1;

    private final List<AdminAiChatbotViewModel.ChatMessage> messages = new ArrayList<>();
    private final ChatActionListener listener;

    public AdminAiChatAdapter(@NonNull ChatActionListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        AdminAiChatbotViewModel.ChatMessage message = messages.get(position);
        return message.sender == AdminAiChatbotViewModel.ChatMessage.Sender.ADMIN
                ? VIEW_TYPE_ADMIN
                : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_ADMIN) {
            ItemAdminChatMessageUserBinding binding = ItemAdminChatMessageUserBinding.inflate(inflater, parent, false);
            return new UserMessageViewHolder(binding);
        }
        ItemAdminChatMessageAiBinding binding = ItemAdminChatMessageAiBinding.inflate(inflater, parent, false);
        return new AiMessageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AdminAiChatbotViewModel.ChatMessage message = messages.get(position);
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
            return;
        }
        ((AiMessageViewHolder) holder).bind(message, listener);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void submitList(@NonNull List<AdminAiChatbotViewModel.ChatMessage> items) {
        messages.clear();
        messages.addAll(items);
        notifyDataSetChanged();
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemAdminChatMessageUserBinding binding;

        UserMessageViewHolder(@NonNull ItemAdminChatMessageUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull AdminAiChatbotViewModel.ChatMessage message) {
            binding.textAdminChatUserMessage.setText(message.content);
        }
    }

    static class AiMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemAdminChatMessageAiBinding binding;

        AiMessageViewHolder(@NonNull ItemAdminChatMessageAiBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull AdminAiChatbotViewModel.ChatMessage message, @NonNull ChatActionListener listener) {
            binding.textAdminChatAiMessage.setText(message.content);

            boolean hasActions = message.showActions;
            binding.layoutAdminChatAiActions.setVisibility(hasActions ? View.VISIBLE : View.GONE);
            if (hasActions) {
                binding.buttonAdminChatAiActionPrimary.setOnClickListener(v -> listener.onDispatchNow());
                binding.buttonAdminChatAiActionSecondary.setOnClickListener(v -> listener.onViewDetails());
            } else {
                binding.buttonAdminChatAiActionPrimary.setOnClickListener(null);
                binding.buttonAdminChatAiActionSecondary.setOnClickListener(null);
            }
        }
    }

    public interface ChatActionListener {
        void onDispatchNow();

        void onViewDetails();
    }
}
