package com.error404.communityvolunteerplatform.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.adapters.MessageAdapter;
import com.error404.communityvolunteerplatform.helpers.NotificationHelper;
import com.error404.communityvolunteerplatform.helpers.UserHelper;
import com.error404.communityvolunteerplatform.models.Message;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesActivity extends AppCompatActivity {

    private String chatId;
    private String otherUserId;
    private String currentUserId;
    private FirebaseFirestore db;
    private ListenerRegistration messagesListener;
    
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private EditText etMessage;
    private ImageButton btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        chatId = getIntent().getStringExtra("chatId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        currentUserId = FirebaseAuth.getInstance().getUid();
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        adapter = new MessageAdapter(messageList, currentUserId);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        if (chatId != null) {
            // We already know exactly which chat to open — use it directly
            startChatSession();
        } else if (otherUserId != null) {
            // We only know the other user — find or create the correct chat
            findOrCreateChat();
        } else {
            Toast.makeText(this, "Error: Chat not initialized", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void startChatSession() {
        resetUnreadCount();
        listenForMessages();

        if (otherUserId != null) {
            fetchOtherUserDetails();
        } else {
            // If otherUserId is null, fetch it from the chat participants
            db.collection("chats").document(chatId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    List<String> participants = (List<String>) doc.get("participants");
                    if (participants != null) {
                        for (String id : participants) {
                            if (!id.equals(currentUserId)) {
                                otherUserId = id;
                                fetchOtherUserDetails();
                                break;
                            }
                        }
                    }
                }
            });
        }
    }

    private void findOrCreateChat() {
        // Query for existing chat containing BOTH users in participants array
        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .get()
            .addOnSuccessListener(snapshots -> {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    List<String> participants = (List<String>) doc.get("participants");
                    if (participants != null && participants.contains(otherUserId)) {
                        // Found existing chat — use it regardless of how it was created
                        chatId = doc.getId();
                        startChatSession();
                        return;
                    }
                }
                // No existing chat found — create new one
                createNewChat();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to open chat", Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    private void createNewChat() {
        List<String> participants = Arrays.asList(currentUserId, otherUserId);
        Map<String, Object> chat = new HashMap<>();
        chat.put("participants", participants);
        chat.put("lastMessage", "");
        chat.put("lastMessageAt", Timestamp.now());
        chat.put("unreadCount_" + currentUserId, 0);
        chat.put("unreadCount_" + otherUserId, 0);
        db.collection("chats").add(chat)
            .addOnSuccessListener(docRef -> {
                chatId = docRef.getId();
                startChatSession();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to create chat", Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    private void fetchOtherUserDetails() {
        UserHelper.fetchDisplayName(otherUserId, (name, pic) -> {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(name);
        });
    }

    private void resetUnreadCount() {
        db.collection("chats").document(chatId)
                .update("unreadCount_" + currentUserId, 0);
    }

    private void listenForMessages() {
        messagesListener = db.collection("chats").document(chatId)
                .collection("messages")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    messageList.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                        Message msg = doc.toObject(Message.class);
                        msg.setMessageId(doc.getId());
                        messageList.add(msg);
                        // Mark received messages as read
                        if (!msg.getSenderId().equals(currentUserId) && !msg.isRead()) {
                            doc.getReference().update("read", true);
                        }
                    }
                    // Sort locally by sentAt
                    messageList.sort((m1, m2) -> {
                        if (m1.getSentAt() == null || m2.getSentAt() == null) return 0;
                        return m1.getSentAt().compareTo(m2.getSentAt());
                    });
                    adapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        etMessage.setText("");

        Map<String, Object> msgData = new HashMap<>();
        msgData.put("senderId", currentUserId);
        msgData.put("text", text);
        msgData.put("sentAt", Timestamp.now());
        msgData.put("read", false);

        db.collection("chats").document(chatId).collection("messages").add(msgData)
                .addOnSuccessListener(docRef -> {
                    Map<String, Object> chatUpdate = new HashMap<>();
                    chatUpdate.put("lastMessage", text);
                    chatUpdate.put("lastMessageAt", Timestamp.now());
                    chatUpdate.put("unreadCount_" + otherUserId, FieldValue.increment(1));
                    chatUpdate.put("unreadCount_" + currentUserId, 0);

                    db.collection("chats").document(chatId).update(chatUpdate);

                    UserHelper.fetchDisplayName(currentUserId, (senderName, pic) -> {
                        NotificationHelper.createNotification(
                                otherUserId,
                                "New Message from " + senderName,
                                text, "chat_message", chatId
                        );
                    });
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }
}
