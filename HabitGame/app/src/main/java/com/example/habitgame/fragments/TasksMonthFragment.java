package com.example.habitgame.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habitgame.R;
import com.example.habitgame.adapters.TaskItemAdapter;
import com.example.habitgame.model.Task;
import com.example.habitgame.services.TaskService;
import com.example.habitgame.utils.DateUtils;
import com.example.habitgame.utils.RecurrenceScheduler;
import com.google.android.material.card.MaterialCardView;

import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder;
import com.kizitonwose.calendar.view.ViewContainer;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.CalendarMonth;
import com.kizitonwose.calendar.core.DayPosition;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kotlin.Unit;

public class TasksMonthFragment extends Fragment implements TaskItemAdapter.Listener {

    private CalendarView calendarView;
    private TextView tvMonthTitle;
    private RecyclerView rvDay;
    private TaskItemAdapter dayAdapter;

    private final TaskService taskService = new TaskService();

    private final Map<Long, List<Task>> tasksByDay = new HashMap<>();
    private Long selectedDayMs = null;

    private static class DayViewContainer extends ViewContainer {
        final View root;
        final MaterialCardView card;
        final TextView tvDayNumber;
        final LinearLayout dotRow;

        DayViewContainer(@NonNull View view) {
            super(view);
            root = view;
            card = view.findViewById(R.id.card_root);
            tvDayNumber = view.findViewById(R.id.tv_day_number);
            dotRow = view.findViewById(R.id.dot_row);
        }
    }

    private static class HeaderContainer extends ViewContainer {
        HeaderContainer(@NonNull View view) { super(view); }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_tasks_month, c, false);

        tvMonthTitle = v.findViewById(R.id.tv_month_title);
        calendarView = v.findViewById(R.id.calendarView);
        rvDay = v.findViewById(R.id.recycler_day_tasks);

        rvDay.setLayoutManager(new LinearLayoutManager(requireContext()));
        dayAdapter = new TaskItemAdapter(this);
        rvDay.setAdapter(dayAdapter);

        // LISTENER za rezultate iz sheet-a → osveži kalendar/listu
        getParentFragmentManager().setFragmentResultListener(
                TaskDetailsBottomSheet.FR_RESULT_KEY, this, (key, res) -> loadTasksAndRender()
        );

        setupCalendar();
        loadTasksAndRender();
        return v;
    }

    private void setupCalendar() {
        YearMonth current = YearMonth.now();
        YearMonth start = current.minusMonths(12);
        YearMonth end   = current.plusMonths(12);
        DayOfWeek firstDay = DayOfWeek.MONDAY;

        calendarView.setup(start, end, firstDay);
        calendarView.scrollToMonth(current);

        calendarView.setMonthHeaderBinder(new MonthHeaderFooterBinder<HeaderContainer>() {
            @NonNull @Override
            public HeaderContainer create(@NonNull View view) { return new HeaderContainer(view); }
            @Override public void bind(@NonNull HeaderContainer container, @NonNull CalendarMonth month) {
                setMonthTitle(month);
            }
        });

        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @NonNull @Override
            public DayViewContainer create(@NonNull View view) { return new DayViewContainer(view); }

            @Override
            public void bind(@NonNull DayViewContainer c, @NonNull CalendarDay day) {
                c.tvDayNumber.setText(String.valueOf(day.getDate().getDayOfMonth()));

                boolean inMonth = (day.getPosition() == DayPosition.MonthDate);
                c.tvDayNumber.setAlpha(inMonth ? 1f : 0.3f);

                long dayMs = toMidnightMs(day.getDate().getYear(),
                        day.getDate().getMonthValue(),
                        day.getDate().getDayOfMonth());
                boolean isSelected = (selectedDayMs != null && selectedDayMs.equals(dayMs));
                c.card.setChecked(isSelected);
                c.card.setStrokeColor(isSelected ? Color.BLACK : Color.DKGRAY);
                c.card.setStrokeWidth(isSelected ? dp(2) : dp(1));

                c.dotRow.removeAllViews();
                List<Task> tasks = tasksByDay.getOrDefault(dayMs, Collections.emptyList());
                LinkedHashSet<Integer> colors = new LinkedHashSet<>();
                for (Task t : tasks) {
                    colors.add(0xFF9E9E9E);
                    if (colors.size() == 3) break;
                }
                int size = dp(6), m = dp(2);
                for (int col : colors) {
                    View dot = new View(requireContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                    lp.leftMargin = m; lp.rightMargin = m;
                    dot.setLayoutParams(lp);
                    dot.setBackgroundResource(R.drawable.shape_circle);
                    dot.getBackground().setTint(col);
                    c.dotRow.addView(dot);
                }

                c.root.setOnClickListener(v -> {
                    if (!inMonth) return;
                    selectedDayMs = dayMs;
                    updateDayList();
                    calendarView.notifyCalendarChanged();
                });
            }
        });

        calendarView.setMonthScrollListener(month -> {
            setMonthTitle(month);
            return Unit.INSTANCE;
        });

        // inicijalna selekcija = danas
        if (selectedDayMs == null) selectedDayMs = DateUtils.startOfToday();
    }

    private void setMonthTitle(@NonNull CalendarMonth month) {
        java.time.format.TextStyle style = java.time.format.TextStyle.FULL;
        String name = month.getYearMonth().getMonth().getDisplayName(style, Locale.ENGLISH);
        tvMonthTitle.setText(name + " " + month.getYearMonth().getYear());
    }

    private void loadTasksAndRender() {
        taskService.getTasksForCurrentUser()
                .addOnSuccessListener(list -> {
                    if (list == null) list = new ArrayList<>();

                    tasksByDay.clear();

                    long from = DateUtils.startOfToday() - 90L * 24 * 60 * 60 * 1000;
                    long to   = DateUtils.startOfToday() + 365L * 24 * 60 * 60 * 1000;

                    for (Task t : list) {
                        if (Boolean.TRUE.equals(t.getIsRepeating())) {
                            for (Task inst : RecurrenceScheduler.expandTaskAsInstances(t, from, to)) {
                                long key = DateUtils.normalizeToMidnight(inst.getExecutionTime());
                                tasksByDay.computeIfAbsent(key, k -> new ArrayList<>()).add(inst);
                            }
                        } else {
                            Long w = (t.getExecutionTime() != null) ? t.getExecutionTime() : t.getStartDate();
                            if (w != null) {
                                long key = DateUtils.normalizeToMidnight(w);
                                tasksByDay.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
                            }
                        }
                    }

                    updateDayList();
                    calendarView.notifyCalendarChanged();
                });
    }

    private void updateDayList() {
        List<Task> dayList = tasksByDay.getOrDefault(selectedDayMs, Collections.emptyList());
        dayAdapter.submitList(new ArrayList<>(dayList));
    }

    // ---- Klik na task iz liste ispod kalendara → OTVORI SHEET ----
    @Override public void onOpen(Task t) {
        Long instanceTime = selectedDayMs; // prosledi pojavu kad dolazi iz kalendara
        TaskDetailsBottomSheet.newInstance(t, instanceTime)
                .show(getParentFragmentManager(), "taskDetails");
    }

    @Override public void onDone(Task t) { new TaskService().markDone(t).addOnSuccessListener(x -> loadTasksAndRender()); }
    @Override public void onCancel(Task t) { new TaskService().markCanceled(t).addOnSuccessListener(x -> loadTasksAndRender()); }
    @Override public void onPause(Task t) { new TaskService().markPaused(t).addOnSuccessListener(x -> loadTasksAndRender()); }
    @Override public void onActive(Task t) { new TaskService().markActive(t).addOnSuccessListener(x -> loadTasksAndRender()); }

    private int dp(int v) {
        float d = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }

    private long toMidnightMs(int year, int month, int day) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.YEAR, year);
        c.set(java.util.Calendar.MONTH, month - 1);
        c.set(java.util.Calendar.DAY_OF_MONTH, day);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
