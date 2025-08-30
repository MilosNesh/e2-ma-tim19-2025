package com.example.habitgame.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.example.habitgame.R;

public class AvatarAdapter extends BaseAdapter {

    private Context context;
    private Integer[] avatars;

    public AvatarAdapter(Context context, Integer[] avatars) {
        this.context = context;
        this.avatars = avatars;
    }

    @Override
    public int getCount() {
        return avatars.length;
    }

    @Override
    public Object getItem(int position) {
        return avatars[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.avatar_item, parent, false);
        }

        ImageView avatarImageView = convertView.findViewById(R.id.avatar_image);
        avatarImageView.setImageResource(avatars[position]);

        return convertView;
    }
}

