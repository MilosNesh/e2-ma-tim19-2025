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
import com.example.habitgame.model.Equipment;

import java.util.List;

public class EquipmentAdapter extends BaseAdapter {

    private Context context;
    private List<Equipment> equipmentList;
    private OnBuyClickListener onBuyClickListener;

    // Interfejs za klik na dugme "Buy"
    public interface OnBuyClickListener {
        void onBuyClick(Equipment product);
    }

    public EquipmentAdapter(Context context, List<Equipment> equipmentList, OnBuyClickListener onBuyClickListener) {
        this.context = context;
        this.equipmentList = equipmentList;
        this.onBuyClickListener = onBuyClickListener;
    }

    @Override
    public int getCount() {
        return equipmentList.size();
    }

    @Override
    public Object getItem(int position) {
        return equipmentList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.equipment, parent, false);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.product_image);
            holder.nameTextView = convertView.findViewById(R.id.product_name);
            holder.descriptionTextView = convertView.findViewById(R.id.product_description);
            holder.priceTextView = convertView.findViewById(R.id.product_price);
            holder.buyButton = convertView.findViewById(R.id.buy_button);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Equipment equipment = equipmentList.get(position);

        holder.nameTextView.setText(equipment.getName());
        holder.descriptionTextView.setText(equipment.getEffect());
        holder.priceTextView.setText(String.format("$%.2f", equipment.getPrice()));
        String imageName = equipment.getImage();
        int resID = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());

        if (resID != 0) {
            holder.imageView.setImageResource(resID);
        } else {
            holder.imageView.setImageResource(R.drawable.avatar1); // Ako slika ne postoji, koristi defaultnu
        }
        // Postavljanje OnClickListener-a za dugme "Buy"
        holder.buyButton.setOnClickListener(v -> {
            if (onBuyClickListener != null) {
                onBuyClickListener.onBuyClick(equipment);
            }
        });

        return convertView;
    }

    // ViewHolder pattern
    static class ViewHolder {
        TextView nameTextView;
        TextView descriptionTextView;
        TextView priceTextView;
        Button buyButton;
        ImageView imageView;
    }
}

