package com.error404.communityvolunteerplatform.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.adapters.MessageAdapter;
import com.error404.communityvolunteerplatform.models.Chat;
import com.error404.communityvolunteerplatform.models.Message;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessagesActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private MessageAdapter adapter;
    private List<Message> messageList;
    
    private FirebaseFirestore db;
    private String currentUserId;
    private String otherUserId;
    private String chatId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        otherUserId = getIntent().getStringExtra("otherUserId");

        if (otherUserId == null) {
            finish();
            return;
        }

        // Generate a deterministic chatId for 1-on-1 chat
        chatId = generateChatId(currentUserId, otherUserId);

        initViews();
        setupRecyclerView();
        loadMessages();
        
        btnSend.setOnClickListener(v -> sendMessage());
        
        fetchOtherUserName();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList, currentUserId);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);
    }

    private String generateChatId(String uid1, String uid2) {
        if (uid1.compareTo(uid2) < 0) {
            return uid1 + "_" + uid2;
        } else {
            return uid2 + "_" + uid1;
        }
    }

    private void loadMessages() {
        db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (value != null) {
                        messageList.clear();
                        for (Message msg : value.toObjects(Message.class)) {
                            messageList.add(msg);
                        }
                        adapter.notifyDataSetChanged();
                        if (messageList.size() > 0) {
                            rvMessages.smoothScrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        Message message = new Message(currentUserId, "", text);
        message.setTimestamp(Timestamp.now());

        // Use a batch to update chat metadata and send message
        DocumentReference chatRef = db.collection("chats").document(chatId);
        
        // Ensure chat document exists
        chatRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Chat chat = new Chat(chatId, Arrays.asList(currentUserId, otherUserId));
                chat.setLastMessage(text);
                chat.setLastMessageAt(Timestamp.now());
                chat.setLastSenderId(currentUserId);
                chatRef.set(chat);
            } else {
                chatRef.update("lastMessage", text, "lastMessageAt", Timestamp.now(), "lastSenderId", currentUserId);
            }
            
            // Add message to subcollection
            chatRef.collection("messages").add(message);
        });

        etMessage.setText("");
    }

    private void fetchOtherUserName() {
        db.collection("users").document(otherUserId).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                String role = userDoc.getString("role");
                if ("volunteer".equals(role)) {
                    db.collection("volunteers").document(otherUserId).get().addOnSuccessListener(volDoc -> {
                        if (volDoc.exists()) {
                            getSupportActionBar().setTitle(volDoc.getString("firstName") + " " + volDoc.getString("surname"));
                        }
                    });
                } else if ("organisation".equals(role)) {
                    db.collection("organisations").document(otherUserId).get().addOnSuccessListener(orgDoc -> {
                        if (orgDoc.exists()) {
                            getSupportActionBar().setTitle(orgDoc.getString("orgName"));
                        }
                    });
                } else {
                    getSupportActionBar().setTitle("Admin (" + userDoc.getString("email") + ")");
                }
            }
        });
    }
}
