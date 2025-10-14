package com.example.habitgame.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habitgame.R;
import com.example.habitgame.model.Category;

public class CategoryAdapter extends ListAdapter<Category, CategoryAdapter.VH> {

    public interface Listener {
        void onEdit(Category c);
        void onDelete(Category c);
    }

    private final Listener listener;

    public CategoryAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Category> DIFF =
            new DiffUtil.ItemCallback<Category>() {
                @Override public boolean areItemsTheSame(@NonNull Category o, @NonNull Category n) {
                    return o.getId()!=null && o.getId().equals(n.getId());
                }
                @Override public boolean areContentsTheSame(@NonNull Category o, @NonNull Category n) {
                    return eq(o.getName(), n.getName()) && eq(o.getColorHex(), n.getColorHex());
                }
                private boolean eq(Object a, Object b){ return a==null? b==null : a.equals(b);}
            };

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_category, p, false);
        return new VH(view);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Category c = getItem(pos);
        h.tvName.setText(c.getName());
        try {
            h.colorView.setBackgroundColor(Color.parseColor(c.getColorHex()));
        } catch (Exception e) {
            h.colorView.setBackgroundColor(0xFF9E9E9E); // fallback
        }
        h.btnEdit.setOnClickListener(v -> listener.onEdit(c));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(c));
    }

    static class VH extends RecyclerView.ViewHolder {
        View colorView; TextView tvName; ImageButton btnEdit, btnDelete;
        VH(@NonNull View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.color_view);
            tvName    = itemView.findViewById(R.id.tv_name);
            btnEdit   = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
