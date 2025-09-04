package com.example.habitgame.adapters;

import static android.app.PendingIntent.getActivity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.habitgame.R;
import com.example.habitgame.model.Account;

import java.util.List;

public class ProfileAdapter extends BaseAdapter {

    private Context context;
    private List<Account> accountList;
    private OnShowClickListener onShowClickListener;
    private String email;

    // Interfejs za klik na dugme za prikaz profla
    public interface OnShowClickListener {
        void onShowClick(Account product);
    }

    public ProfileAdapter(Context context, List<Account> accountList, String email, OnShowClickListener onShowClickListener) {
        this.context = context;
        this.accountList = accountList;
        this.email = email;
        this.onShowClickListener = onShowClickListener;
    }

    public void updateList(List<Account> newAccountList) {
        accountList.clear();
        accountList.addAll(newAccountList);
    }

    public void clearList() {
        accountList.clear();
    }
    @Override
    public int getCount() {
        return accountList.size();
    }

    @Override
    public Object getItem(int position) {
        return accountList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.friend, parent, false);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.avatar);
            holder.usernameTextView = convertView.findViewById(R.id.profile_username);
            holder.isFriendTextView = convertView.findViewById(R.id.is_friend);
            holder.showButton = convertView.findViewById(R.id.show_profile);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if(accountList != null) {
            Account account = accountList.get(position);

            holder.usernameTextView.setText(account.getUsername());
            if(account.getFriends().contains(email))
                holder.isFriendTextView.setText("Prijatelj");
            else
                holder.isFriendTextView.setText("Niste prijatelji");
            holder.imageView.setImageResource(account.getAvatar());

            holder.showButton.setOnClickListener(v -> {
                if (onShowClickListener != null) {
                    onShowClickListener.onShowClick(account);
                }
            });
        }
        return convertView;
    }

    // ViewHolder pattern
    static class ViewHolder {
        TextView usernameTextView;
        TextView isFriendTextView;
        Button showButton;
        ImageView imageView;
    }
}

