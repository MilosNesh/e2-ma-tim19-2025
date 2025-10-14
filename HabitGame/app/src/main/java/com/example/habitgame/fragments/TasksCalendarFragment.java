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
import com.example.habitgame.repositories.CategoryRepository;
import com.example.habitgame.repositories.TaskRepository;

import java.util.*;

public class TasksCalendarFragment extends Fragment {

    private WeekView weekView;

    private final Map<String, String> categoryColors = new HashMap<>();
    private List<Task> allTasks = new ArrayList<>();

    public static class MyEvent {
        public final long id;
        public final String title;
        public final Calendar start;
        public final Calendar end;
        public final int color;
        public final Task taskRef;

        public MyEvent(long id, String title, Calendar start, Calendar end, int color, Task taskRef) {
            this.id = id;
            this.title = title;
            this.start = start;
            this.end = end;
            this.color = color;
            this.taskRef = taskRef;
        }
    }

    private class MySimpleAdapter extends WeekView.SimpleAdapter<MyEvent> {

        @NonNull
        @Override
        public WeekViewEntity onCreateEntity(@NonNull MyEvent item) {
            WeekViewEntity.Style style = new WeekViewEntity.Style.Builder()
                    .setBackgroundColor(item.color)
                    .setTextColor(Color.WHITE)
                    .build();

            return new WeekViewEntity.Event.Builder(item)
                    .setId(item.id)
                    .setTitle(item.title)
                    .setStartTime(item.start)
                    .setEndTime(item.end)
                    .setStyle(style)
                    .build();
        }

        @Override
        public void onEventClick(@NonNull MyEvent data) {
            TaskDetailsBottomSheet.newInstance(data.taskRef)
                    .show(getParentFragmentManager(), "taskDetails");
        }
    }

    private MySimpleAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inf.inflate(R.layout.fragment_tasks_calendar, container, false);
        weekView = v.findViewById(R.id.weekView);

        adapter = new MySimpleAdapter();
        weekView.setAdapter(adapter);

        loadDataThenRender();
        return v;
    }

    private void loadDataThenRender() {
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
                    loadTasks();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Greška kategorije: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadTasks() {
        TaskRepository.getTasksForCurrentUser()
                .addOnSuccessListener(list -> {
                    allTasks = (list != null) ? list : new ArrayList<>();

                    Calendar from = Calendar.getInstance();
                    from.add(Calendar.DAY_OF_YEAR, -30);
                    Calendar to = Calendar.getInstance();
                    to.add(Calendar.DAY_OF_YEAR, 60);

                    List<MyEvent> events = buildEventsForRange(from, to);
                    adapter.submitList(events);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Greška zadaci: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private List<MyEvent> buildEventsForRange(Calendar rangeStart, Calendar rangeEnd) {
        List<MyEvent> out = new ArrayList<>();

        for (Task t : allTasks) {
            int color = 0xFF9E9E9E;
            if (t.getCategoryId() != null) {
                String hex = categoryColors.get(t.getCategoryId());
                if (hex != null && hex.matches("^#[0-9A-Fa-f]{6}$")) {
                    try { color = Color.parseColor(hex); } catch (Exception ignored) {}
                }
            }

            if (t.getExecutionTime() != null) {
                Calendar s = millisToCal(t.getExecutionTime());
                Calendar e = (Calendar) s.clone();
                e.add(Calendar.MINUTE, 60);
                addIfInRange(out, t, s, e, color, rangeStart, rangeEnd);
            } else if (t.getStartDate() != null) {
                Calendar s = millisToCal(t.getStartDate());
                Calendar e = (t.getEndDate() != null) ? millisToCal(t.getEndDate()) : (Calendar) s.clone();
                if (t.getEndDate() == null) e.add(Calendar.MINUTE, 60);

                if (!t.getIsRepeating()) {
                    addIfInRange(out, t, s, e, color, rangeStart, rangeEnd);
                } else {
                    int step = (t.getRepeatInterval() != null && t.getRepeatInterval() > 0) ? t.getRepeatInterval() : 1;
                    String unit = t.getRepeatUnit() != null ? t.getRepeatUnit().toLowerCase(Locale.ROOT) : "dan";

                    Calendar curS = (Calendar) s.clone();
                    Calendar curE = (Calendar) e.clone();

                    Calendar hardEnd = null;
                    if (t.getEndDate() != null) hardEnd = millisToCal(t.getEndDate());

                    while (curS.before(rangeEnd)) {
                        if (curE.after(rangeStart)) {
                            addIfInRange(out, t, curS, curE, color, rangeStart, rangeEnd);
                        }
                        switch (unit) {
                            case "nedelja":
                            case "week":
                                curS.add(Calendar.WEEK_OF_YEAR, step); curE.add(Calendar.WEEK_OF_YEAR, step); break;
                            case "mesec":
                            case "month":
                                curS.add(Calendar.MONTH, step);       curE.add(Calendar.MONTH,       step); break;
                            case "dan":
                            case "day":
                            default:
                                curS.add(Calendar.DAY_OF_YEAR, step);  curE.add(Calendar.DAY_OF_YEAR, step);  break;
                        }
                        if (hardEnd != null && curS.after(hardEnd)) break;
                    }
                }
            }
        }
        return out;
    }

    private void addIfInRange(List<MyEvent> out, Task t, Calendar s, Calendar e, int color,
                              Calendar from, Calendar to) {
        if (e.before(from) || s.after(to)) return;

        long id = (t.getId() != null) ? t.getId().hashCode() : System.identityHashCode(t);
        String title = (t.getName() != null) ? t.getName() : "Zadatak";
        if (t.getXpValue() > 0) title = title + " · XP " + t.getXpValue();

        out.add(new MyEvent(id, title, (Calendar) s.clone(), (Calendar) e.clone(), color, t));
    }

    private Calendar millisToCal(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c;
    }
}
