package com.example.habitgame.fragments;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.example.habitgame.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TasksPagerFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_task_pager, c, false);
        TabLayout tabs = v.findViewById(R.id.tab_layout);
        ViewPager2 pager = v.findViewById(R.id.view_pager);

        pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int pos) {
                if (pos == 0) return new TaskListFragment();              // Jednokratni
                else return new RepeatedTaskOccurrenceListFragment();                 // Pojave ponavljajućih
            }
            @Override public int getItemCount() { return 2; }
        });

        new TabLayoutMediator(tabs, pager, (tab, pos) ->
                tab.setText(pos == 0 ? R.string.one_time_tasks : R.string.repeating_tasks)
        ).attach();

        return v;
    }
}
