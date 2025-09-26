package com.example.habitgame.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.habitgame.R;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MessageAdapter extends BaseAdapter {
    private Context context;
    private List<Message> messageList;
    private String email;

    public MessageAdapter(Context context, List<Message> messageList, String email) {
        this.context = context;
        this.messageList = messageList;
        this.email = email;
    }
    public void updateList(List<Message> newMessageList) {
        messageList.clear();
        messageList.addAll(newMessageList);
    }

    public void clearList() {
        messageList.clear();
    }
    @Override
    public int getCount() {
        return messageList.size();
    }

    @Override
    public Object getItem(int position) {
        return messageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MessageAdapter.ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.message, parent, false);
            holder = new MessageAdapter.ViewHolder();
            holder.usernameTextView = convertView.findViewById(R.id.authorUsername);
            holder.dateTextView = convertView.findViewById(R.id.messageDate);
            holder.textTextView = convertView.findViewById(R.id.messageText);
            holder.messageContainer = convertView.findViewById(R.id.messageContainer);
            holder.messageHeader = convertView.findViewById(R.id.messageHeader);
            convertView.setTag(holder);
        } else {
            holder = (MessageAdapter.ViewHolder) convertView.getTag();
        }

        if(messageList != null) {
            Message message = messageList.get(position);

            holder.usernameTextView.setText(message.getAuthorUsername());

            LocalDateTime localDateTime = message.getDateAsLocalDateTime();
            if (localDateTime != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                String formattedDate = localDateTime.format(formatter);
                holder.dateTextView.setText(formattedDate);
            }
            holder.textTextView.setText(message.getText());

            if(message.getAuthorEmail().equals(email)) {
                holder.messageContainer.setGravity(Gravity.END);
                holder.messageHeader.setGravity(Gravity.END);
                int color = ContextCompat.getColor(context, R.color.blue_gray);
                holder.messageContainer.setBackgroundColor(color);
            }
            else {
                holder.messageContainer.setGravity(Gravity.START);
                holder.messageHeader.setGravity(Gravity.START);
                int color = ContextCompat.getColor(context, R.color.ash_gray);
                holder.messageContainer.setBackgroundColor(color);            }
        }
        return convertView;
    }
    static class ViewHolder {
        TextView usernameTextView;
        TextView dateTextView;
        TextView textTextView;
        LinearLayout messageContainer;
        LinearLayout messageHeader;
    }
}
