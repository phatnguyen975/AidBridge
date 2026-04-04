package com.drc.aidbridge.ui.main.viewmodel.admin;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.R;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

@HiltViewModel
public class AdminAiChatbotViewModel extends BaseViewModel {

	private final MutableLiveData<List<ChatMessage>> chatMessages = new MutableLiveData<>(new ArrayList<>());
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Context context;

	@Inject
	public AdminAiChatbotViewModel(@ApplicationContext @NonNull Context context) {
		this.context = context;
		// TODO: Kết nối API Gemini/Backend để xử lý hội thoại thực tế
	}

	public LiveData<List<ChatMessage>> getChatMessages() {
		return chatMessages;
	}

	public void loadChatHistory() {
		List<ChatMessage> mock = new ArrayList<>();
		mock.add(new ChatMessage(
                ChatMessage.Sender.AI,
				context.getString(R.string.admin_ai_chatbot_mock_ai_welcome),
				false));
		mock.add(new ChatMessage(
                ChatMessage.Sender.ADMIN,
				context.getString(R.string.admin_ai_chatbot_mock_admin_question),
				false));
		mock.add(new ChatMessage(
                ChatMessage.Sender.AI,
				context.getString(R.string.admin_ai_chatbot_mock_ai_suggestion),
				true));
		chatMessages.setValue(mock);
	}

	public void sendNewMessage(@NonNull String text) {
		List<ChatMessage> current = chatMessages.getValue();
		List<ChatMessage> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);
		updated.add(new ChatMessage(ChatMessage.Sender.ADMIN, text, false));
		chatMessages.setValue(updated);

		handler.postDelayed(() -> {
			List<ChatMessage> latest = chatMessages.getValue();
			List<ChatMessage> latestUpdated = latest == null ? new ArrayList<>() : new ArrayList<>(latest);
			latestUpdated.add(new ChatMessage(
                    ChatMessage.Sender.AI,
					context.getString(R.string.admin_ai_chatbot_mock_ai_auto_reply),
					true));
			chatMessages.setValue(latestUpdated);
		}, 1000);
	}

	public static final class ChatMessage {
		public enum Sender {
			ADMIN,
			AI
		}

		@NonNull
		public final Sender sender;
		@NonNull
		public final String content;
		public final boolean showActions;

		public ChatMessage(@NonNull Sender sender, @NonNull String content, boolean showActions) {
			this.sender = sender;
			this.content = content;
			this.showActions = showActions;
		}
	}
}
