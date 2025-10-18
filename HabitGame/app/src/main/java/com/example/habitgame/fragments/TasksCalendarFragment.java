package com.example.habitgame.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEntity;
import com.example.habitgame.R;
import com.example.habitgame.model.Category;
import com.example.habitgame.model.Task;
import com.example.habitgame.model.RepeatedTaskOccurence;
import com.example.habitgame.repositories.CategoryRepository;
import com.example.habitgame.repositories.TaskRepository;
import com.example.habitgame.repositories.RepeatedTaskOccurrenceRepository;

import java.util.*;

/**
 * Kombinovano: renderuje i Task i RepeatedTaskOccurrence u WeekView bez dodatnih "event" klasa.
 */
public class TasksCalendarFragment extends Fragment {

    private WeekView weekView;

    private final Map<String, String> categoryColors = new HashMap<>();
    private List<Task> allTasks = new ArrayList<>();
    private List<RepeatedTaskOccurence> allOcc = new ArrayList<>();

    private class MyAdapter extends WeekView.SimpleAdapter<Object> {
        @NonNull @Override
        public WeekViewEntity onCreateEntity(@NonNull Object item) {
            String title;
            Calendar start;
            Calendar end;
            int color = 0xFF9E9E9E;

            if (item instanceof Task) {
                Task t = (Task) item;
                title = (t.getName() != null ? t.getName() : "Zadatak");
                if (t.getXpValue() > 0) title = title + " · XP " + t.getXpValue();

                Long when = (t.getExecutionTime() != null) ? t.getExecutionTime() : t.getStartDate();
                if (when == null) when = System.currentTimeMillis();
                start = millisToCal(when);
                end = (Calendar) start.clone();
                end.add(Calendar.MINUTE, 60);

                // boja po kategoriji:
                if (t.getCategoryId() != null) {
                    String hex = categoryColors.get(t.getCategoryId());
                    if (hex != null && hex.matches("^#[0-9A-Fa-f]{6}$")) {
                        try { color = Color.parseColor(hex); } catch (Exception ignored){}
                    }
                }
            } else {
                RepeatedTaskOccurence oc = (RepeatedTaskOccurence) item;
                title = (oc.getTaskName() != null ? oc.getTaskName() : "Ponavljajući");
                if (oc.getXp() > 0) title = title + " · XP " + oc.getXp();
                long when = (oc.getWhen() != null ? oc.getWhen() : System.currentTimeMillis());
                start = millisToCal(when);
                end = (Calendar) start.clone();
                end.add(Calendar.MINUTE, 60);
                // boju po kategoriji možeš dodati u Occurrence model (categoryId) i mapirati ovde, ako je imaš
            }

            WeekViewEntity.Style style = new WeekViewEntity.Style.Builder()
                    .setBackgroundColor(color)
                    .setTextColor(Color.WHITE)
                    .build();

            return new WeekViewEntity.Event.Builder(item)
                    .setId(System.identityHashCode(item))
                    .setTitle(title)
                    .setStartTime(start)
                    .setEndTime(end)
                    .setStyle(style)
                    .build();
        }

        @Override
        public void onEventClick(@NonNull Object data) {
            if (data instanceof Task) {
                TaskDetailsBottomSheet.newInstance((Task) data)
                        .show(getParentFragmentManager(), "taskDetails");
            } else if (data instanceof RepeatedTaskOccurence) {
                // Ako već imaš poseban bottom sheet za occurrence — pozovi ga ovde.
                RepeatedTaskOccurrenceDetailsBottomSheet.newInstance((RepeatedTaskOccurence) data)
                        .show(getParentFragmentManager(), "occDetails");
            }
        }
    }

    private MyAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inf.inflate(R.layout.fragment_tasks_calendar, container, false);
        weekView = v.findViewById(R.id.weekView);

        adapter = new MyAdapter();
        weekView.setAdapter(adapter);

        loadDataThenRender();
        return v;
    }

    private void loadDataThenRender() {
        // 1) Učitaj boje kategorija
        CategoryRepository.getForCurrentUser()
                .addOnSuccessListener(list -> {
                    categoryColors.clear();
                    if (list != null) {
                        for (Category c : list) {
                            if (c.getId() != null && c.getColorHex() != null) {
                                try { Color.parseColor(c.getColorHex());
                                    categoryColors.put(c.getId(), c.getColorHex());
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                    // 2) Učitaj taskove i occurrence pa prikaži
                    loadItems();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Greška kategorije: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadItems() {
        Calendar from = Calendar.getInstance();
        from.add(Calendar.DAY_OF_YEAR, -30);
        Calendar to = Calendar.getInstance();
        to.add(Calendar.DAY_OF_YEAR, 60);

        long fromMs = from.getTimeInMillis();
        long toMs = to.getTimeInMillis();

        // Tasks:
        TaskRepository.getTasksForCurrentUser()
                .addOnSuccessListener(list -> {
                    allTasks = (list != null) ? list : new ArrayList<>();
                    // Filtriraj na raspon, obične i ponavljajuće **osnovu**; instance se u kalendaru crtaju kao “slot” bez ekspanzije
                    List<Object> render = new ArrayList<>();

                    for (Task t : allTasks) {
                        Long when = (t.getExecutionTime() != null) ? t.getExecutionTime() : t.getStartDate();
                        if (when == null) continue;
                        if (when >= fromMs && when <= toMs) render.add(t);
                    }

                    // Occurrences:
                    RepeatedTaskOccurrenceRepository.getForCurrentUserBetween(fromMs, toMs)
                            .addOnSuccessListener(occ -> {
                                allOcc = (occ != null) ? occ : new ArrayList<>();
                                render.addAll(allOcc);
                                adapter.submitList(render);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Greška occurrence: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Greška zadaci: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private Calendar millisToCal(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c;
    }
}
