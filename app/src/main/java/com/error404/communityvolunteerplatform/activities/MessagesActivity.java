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
import com.error404.communityvolunteerplatform.models.Message;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
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

        if (chatId == null || otherUserId == null || currentUserId == null) {
            finish();
            return;
        }

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

        fetchOtherUserDetails();
        resetUnreadCount();
        listenForMessages();

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void fetchOtherUserDetails() {
        db.collection("users").document(otherUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("fullName");
                if (name == null) name = doc.getString("orgName");
                if (name != null) setTitle(name);
            }
        });
    }

    private void resetUnreadCount() {
        db.collection("chats").document(chatId)
                .update("unreadCount_" + currentUserId, 0);
    }

    private void listenForMessages() {
        messagesListener = db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Message msg = dc.getDocument().toObject(Message.class);
                                msg.setMessageId(dc.getDocument().getId());
                                messageList.add(msg);
                                adapter.notifyItemInserted(messageList.size() - 1);
                                rvMessages.scrollToPosition(messageList.size() - 1);

                                // Mark as read if received
                                if (!msg.getSenderId().equals(currentUserId) && !msg.isRead()) {
                                    dc.getDocument().getReference().update("read", true);
                                }
                            } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                Message msg = dc.getDocument().toObject(Message.class);
                                String id = dc.getDocument().getId();
                                for (int i = 0; i < messageList.size(); i++) {
                                    if (messageList.get(i).getMessageId().equals(id)) {
                                        messageList.set(i, msg);
                                        adapter.notifyItemChanged(i);
                                        break;
                                    }
                                }
                            }
                        }
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
