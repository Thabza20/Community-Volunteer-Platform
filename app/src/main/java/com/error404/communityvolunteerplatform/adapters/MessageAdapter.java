package com.error404.communityvolunteerplatform.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.models.Message;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 0;
    private static final int VIEW_TYPE_RECEIVED = 1;

    private final List<Message> messageList;
    private final String currentUserId;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        if (messageList.get(position).getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText, tvMessageTime, tvReadStatus;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            tvReadStatus = itemView.findViewById(R.id.tvReadStatus);
        }

        void bind(Message message) {
            tvMessageText.setText(message.getText());
            if (message.getSentAt() != null) {
                tvMessageTime.setText(sdf.format(message.getSentAt().toDate()));
            }
            if (message.isRead()) {
                tvReadStatus.setText("✓✓");
                tvReadStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.passport_card_accent));
            } else {
                tvReadStatus.setText("✓");
                tvReadStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.text_secondary));
            }
        }
    }

    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText, tvMessageTime;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
        }

        void bind(Message message) {
            tvMessageText.setText(message.getText());
            if (message.getSentAt() != null) {
                tvMessageTime.setText(sdf.format(message.getSentAt().toDate()));
            }
        }
    }
}
