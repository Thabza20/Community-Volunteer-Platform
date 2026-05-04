package com.error404.communityvolunteerplatform.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.helpers.GroqRecommendationHelper;
import com.error404.communityvolunteerplatform.models.AiChatMessage;
import com.error404.communityvolunteerplatform.models.Opportunity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiChatbotActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageButton btnSend;
    private ProgressBar pbLoading;
    private ChatAdapter adapter;
    private List<AiChatMessage> messages = new ArrayList<>();
    private List<Map<String, String>> conversationHistory = new ArrayList<>();
    private String opportunitiesContext = "";
    private String volunteerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chatbot);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvChat = findViewById(R.id.rv_chat);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        pbLoading = findViewById(R.id.pb_loading);

        volunteerId = FirebaseAuth.getInstance().getUid();

        adapter = new ChatAdapter(messages);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        loadContextAndWelcome();

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void loadContextAndWelcome() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("opportunities")
                .whereEqualTo("status", "active")
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder sb = new StringBuilder();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Opportunity opp = doc.toObject(Opportunity.class);
                        sb.append(opp.getTitle()).append(" (").append(opp.getCategory()).append("), ");
                    }
                    opportunitiesContext = sb.toString();
                    
                    addBotMessage("Hi! I'm your AI Volunteer Assistant. How can I help you today? I can suggest opportunities or help you track your progress!");
                });
    }

    private void sendMessage() {
        String userText = etMessage.getText().toString().trim();
        if (userText.isEmpty()) return;

        etMessage.setText("");
        addUserMessage(userText);
        
        pbLoading.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        GroqRecommendationHelper.sendChatMessage(volunteerId, userText, conversationHistory, opportunitiesContext, new GroqRecommendationHelper.OnChatListener() {
            @Override
            public void onSuccess(String reply) {
                pbLoading.setVisibility(View.GONE);
                btnSend.setEnabled(true);
                addBotMessage(reply);
            }

            @Override
            public void onError(String message) {
                pbLoading.setVisibility(View.GONE);
                btnSend.setEnabled(true);
                Toast.makeText(AiChatbotActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addUserMessage(String text) {
        messages.add(new AiChatMessage(text, AiChatMessage.TYPE_USER));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);

        Map<String, String> msgMap = new HashMap<>();
        msgMap.put("role", "user");
        msgMap.put("content", text);
        conversationHistory.add(msgMap);
    }

    private void addBotMessage(String text) {
        messages.add(new AiChatMessage(text, AiChatMessage.TYPE_BOT));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);

        Map<String, String> msgMap = new HashMap<>();
        msgMap.put("role", "assistant");
        msgMap.put("content", text);
        conversationHistory.add(msgMap);
    }

    private static class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<AiChatMessage> chatMessages;

        ChatAdapter(List<AiChatMessage> chatMessages) {
            this.chatMessages = chatMessages;
        }

        @Override
        public int getItemViewType(int position) {
            return chatMessages.get(position).getType();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == AiChatMessage.TYPE_USER) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
                return new UserViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bot, parent, false);
                return new BotViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            AiChatMessage message = chatMessages.get(position);
            if (holder instanceof UserViewHolder) {
                ((UserViewHolder) holder).tvMessage.setText(message.getText());
            } else {
                ((BotViewHolder) holder).tvMessage.setText(message.getText());
            }
        }

        @Override
        public int getItemCount() {
            return chatMessages.size();
        }

        static class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage;
            UserViewHolder(View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tv_user_message);
            }
        }

        static class BotViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage;
            BotViewHolder(View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tv_bot_message);
            }
        }
    }
}