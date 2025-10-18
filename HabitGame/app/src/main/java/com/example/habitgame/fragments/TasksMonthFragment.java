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
import com.example.habitgame.model.Category;
import com.example.habitgame.model.Task;
import com.example.habitgame.model.TaskStatus;
import com.example.habitgame.model.RepeatedTaskOccurence;
import com.example.habitgame.repositories.CategoryRepository;
import com.example.habitgame.repositories.RepeatedTaskOccurrenceRepository;
import com.example.habitgame.services.TaskService;
import com.example.habitgame.services.RepeatedTaskOccurrenceService;
import com.example.habitgame.utils.DateUtils;
import com.example.habitgame.repositories.TaskRepository;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder;
import com.kizitonwose.calendar.view.ViewContainer;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.CalendarMonth;
import com.kizitonwose.calendar.core.DayPosition;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.*;

import kotlin.Unit;

public class TasksMonthFragment extends Fragment implements TaskItemAdapter.Listener {

    private CalendarView calendarView;
    private TextView tvMonthTitle;
    private RecyclerView rvDay;
    private TaskItemAdapter dayAdapter;

    /** categoryId -> #RRGGBB */
    private final Map<String, String> categoryColors = new HashMap<>();

    private final TaskService taskService = new TaskService();
    private final RepeatedTaskOccurrenceService occService = new RepeatedTaskOccurrenceService();

    /** dan (00:00) -> lista za prikaz */
    private final Map<Long, List<Task>> displayByDay = new HashMap<>();
    private Long selectedDayMs = null;

    // ---- Calendar view holders ----
    private static class DayViewContainer extends ViewContainer {
        final View root;
        final com.google.android.material.card.MaterialCardView card;
        final TextView tvDayNumber;
        final LinearLayout dotRow;
        // NOVO: opcioni “pause” badge (ako postoji u item_calendar_day.xml)
        final View badgePause;

        DayViewContainer(@NonNull View view) {
            super(view);
            root = view;
            card = view.findViewById(R.id.card_root);
            tvDayNumber = view.findViewById(R.id.tv_day_number);
            dotRow = view.findViewById(R.id.dot_row);
            badgePause = view.findViewById(R.id.badge_pause); // može biti null ako ga nema u layoutu
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

        // slušaj rezultat iz bottom-sheeta (pauza/aktivacija/done/cancel) → refresh
        getParentFragmentManager().setFragmentResultListener("series_toggle", this,
                (reqKey, bundle) -> { if (Boolean.TRUE.equals(bundle.getBoolean("changed"))) loadData(); });
        getParentFragmentManager().setFragmentResultListener("occ_changed", this,
                (reqKey, bundle) -> { if (Boolean.TRUE.equals(bundle.getBoolean("changed"))) loadData(); });

        setupCalendar();
        loadData();

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
            @NonNull @Override public HeaderContainer create(@NonNull View view) { return new HeaderContainer(view); }
            @Override public void bind(@NonNull HeaderContainer container, @NonNull CalendarMonth month) {
                setMonthTitle(month);
            }
        });

        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @NonNull @Override public DayViewContainer create(@NonNull View view) { return new DayViewContainer(view); }

            @Override public void bind(@NonNull DayViewContainer c, @NonNull CalendarDay day) {
                c.tvDayNumber.setText(String.valueOf(day.getDate().getDayOfMonth()));

                boolean inMonth = (day.getPosition() == DayPosition.MonthDate);
                c.tvDayNumber.setAlpha(inMonth ? 1f : 0.3f);

                long dayMs = toMidnightMs(day.getDate().getYear(),
                        day.getDate().getMonthValue(),
                        day.getDate().getDayOfMonth());
                boolean isSelected = (selectedDayMs != null && selectedDayMs == dayMs);
                c.card.setChecked(isSelected);
                c.card.setStrokeColor(isSelected ? Color.BLACK : Color.DKGRAY);
                c.card.setStrokeWidth(isSelected ? dp(2) : dp(1));

                // tačkice (max 3) – boja kategorije; PAUZIRAN => smanjena alfa
                c.dotRow.removeAllViews();
                List<Task> items = displayByDay.getOrDefault(dayMs, Collections.emptyList());
                int shown = 0;
                int size = dp(6), m = dp(2);
                for (Task t : items) {
                    if (shown == 3) break;
                    int col = colorForTask(t);
                    float alpha = (t.getStatus() == TaskStatus.PAUZIRAN) ? 0.35f : 1f;

                    View dot = new View(requireContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                    lp.leftMargin = m; lp.rightMargin = m;
                    dot.setLayoutParams(lp);
                    dot.setBackgroundResource(R.drawable.shape_circle);
                    dot.getBackground().setTint(col);
                    dot.setAlpha(alpha);
                    c.dotRow.addView(dot);
                    shown++;
                }

                // NOVO: badge i okvir za pauzu
                if (!items.isEmpty()) {
                    boolean hasPaused = false;
                    boolean allPaused = true;
                    for (Task t : items) {
                        TaskStatus st = t.getStatus() == null ? TaskStatus.AKTIVAN : t.getStatus();
                        if (st == TaskStatus.PAUZIRAN) hasPaused = true;
                        if (st != TaskStatus.PAUZIRAN) allPaused = false;
                    }

                    // prikaži mali badge ako postoji makar jedan pauziran
                    if (c.badgePause != null) {
                        c.badgePause.setVisibility(hasPaused ? View.VISIBLE : View.GONE);
                        if (hasPaused) {
                            // narandžasta (isti ton kao u listama)
                            c.badgePause.getBackground().setTint(Color.parseColor("#EF6C00"));
                        }
                    }

                    // ako su SVI tog dana pauzirani → oboji okvir narandžasto
                    if (allPaused) {
                        c.card.setStrokeColor(Color.parseColor("#EF6C00"));
                        c.card.setStrokeWidth(isSelected ? dp(2) : dp(1));
                    }
                } else {
                    // nema stavki – sakrij badge ako postoji
                    if (c.badgePause != null) c.badgePause.setVisibility(View.GONE);
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
    }

    private int colorForTask(@NonNull Task t){
        String hex = categoryColors.get(t.getCategoryId());
        try {
            return (hex == null || hex.isEmpty()) ? Color.parseColor("#9E9E9E") : Color.parseColor(hex);
        } catch (Exception e){
            return Color.parseColor("#9E9E9E");
        }
    }

    private void setMonthTitle(@NonNull CalendarMonth month) {
        java.time.format.TextStyle style = java.time.format.TextStyle.FULL;
        String name = month.getYearMonth().getMonth().getDisplayName(style, java.util.Locale.ENGLISH);
        tvMonthTitle.setText(name + " " + month.getYearMonth().getYear());
    }

    public void loadData() {
        displayByDay.clear();

        long from = DateUtils.startOfToday() - 90L * 24 * 60 * 60 * 1000;
        long to   = DateUtils.startOfToday() + 365L * 24 * 60 * 60 * 1000;

        CategoryRepository.getForCurrentUser()
                .addOnSuccessListener(cats -> {
                    categoryColors.clear();
                    if (cats != null) for (Category c : cats) {
                        if (c.getId()!=null && c.getColorHex()!=null) categoryColors.put(c.getId(), c.getColorHex());
                    }

                    TaskRepository.getTasksForCurrentUser()
                            .addOnSuccessListener(tasks -> {
                                if (tasks == null) tasks = new ArrayList<>();

                                // jednokratni/bazni taskovi
                                for (Task t : tasks) {
                                    Long w = (t.getExecutionTime() != null) ? t.getExecutionTime() : t.getStartDate();
                                    if (w == null) continue;
                                    if (w < from || w > to) continue;
                                    long key = DateUtils.normalizeToMidnight(w);
                                    displayByDay.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
                                }

                                // occurrences
                                RepeatedTaskOccurrenceRepository.getForCurrentUserBetween(from, to)
                                        .addOnSuccessListener(occList -> {
                                            if (occList != null) {
                                                for (RepeatedTaskOccurence oc : occList) {
                                                    long w = (oc.getWhen() != null) ? oc.getWhen() : DateUtils.startOfToday();
                                                    long key = DateUtils.normalizeToMidnight(w);
                                                    displayByDay.computeIfAbsent(key, k -> new ArrayList<>())
                                                            .add(mapOccurrenceToDisplayTask(oc));
                                                }
                                            }
                                            if (selectedDayMs == null) selectedDayMs = DateUtils.startOfToday();
                                            updateDayList();
                                            calendarView.notifyCalendarChanged();
                                        })
                                        .addOnFailureListener(e -> {
                                            if (selectedDayMs == null) selectedDayMs = DateUtils.startOfToday();
                                            updateDayList();
                                            calendarView.notifyCalendarChanged();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                if (selectedDayMs == null) selectedDayMs = DateUtils.startOfToday();
                                updateDayList();
                                calendarView.notifyCalendarChanged();
                            });
                })
                .addOnFailureListener(e -> {
                    if (selectedDayMs == null) selectedDayMs = DateUtils.startOfToday();
                    updateDayList();
                    calendarView.notifyCalendarChanged();
                });
    }

    private Task mapOccurrenceToDisplayTask(@NonNull RepeatedTaskOccurence oc) {
        Task t = new Task();
        String sid = (oc.getRepeatedTaskId() == null ? "" : oc.getRepeatedTaskId());
        t.setId("occ:" + oc.getId() + ":" + sid);

        t.setUserId(oc.getUserId());
        t.setName(oc.getTaskName() != null ? oc.getTaskName() : getString(R.string.repeating));
        t.setDescription(oc.getTaskDescription());
        t.setCategoryId(oc.getCategoryId());
        t.setWeight(null);
        t.setImportance(null);
        t.setXpValue(Math.max(0, oc.getXp()));
        t.setExecutionTime(oc.getWhen());
        t.setIsRepeating(false);
        t.setStartDate(oc.getWhen());
        t.setEndDate(null);
        t.setRepeatInterval(null);
        t.setRepeatUnit(null);
        t.setIsCompleted(oc.isCompleted());
        t.setCreationTimestamp(oc.getCreatedAt());
        t.setLastCompletionTimestamp(oc.getCompletedAt());
        t.setStatus(oc.getStatus() != null ? oc.getStatus() : TaskStatus.AKTIVAN);
        return t;
    }

    private RepeatedTaskOccurence mapDisplayTaskIdToOccurrence(@NonNull Task t) {
        RepeatedTaskOccurence oc = new RepeatedTaskOccurence();
        String id = t.getId();
        String occId = null;
        String seriesId = null;
        if (id != null && id.startsWith("occ:")) {
            String[] parts = id.split(":");
            if (parts.length >= 2) occId = parts[1];
            if (parts.length >= 3 && !parts[2].isEmpty()) seriesId = parts[2];
        }
        oc.setId(occId);
        oc.setRepeatedTaskId(seriesId);
        oc.setUserId(t.getUserId());
        oc.setTaskName(t.getName());
        oc.setTaskDescription(t.getDescription());
        oc.setWhen(t.getExecutionTime());
        oc.setXp(t.getXpValue());
        oc.setCompleted(Boolean.TRUE.equals(t.getIsCompleted()));
        oc.setCompletedAt(t.getLastCompletionTimestamp());
        oc.setStatus(t.getStatus());
        oc.setCategoryId(t.getCategoryId());
        return oc;
    }

    private void updateDayList() {
        List<Task> dayList = displayByDay.getOrDefault(selectedDayMs, Collections.emptyList());
        dayAdapter.submitList(new ArrayList<>(dayList));
    }

    // ---- TaskItemAdapter.Listener ----
    @Override public void onOpen(Task t) {
        if (isOccurrenceDisplay(t)) {
            RepeatedTaskOccurrenceDetailsBottomSheet.newInstance(mapDisplayTaskIdToOccurrence(t))
                    .show(getParentFragmentManager(), "occDetails");
        } else {
            TaskDetailsBottomSheet.newInstance(t).show(getParentFragmentManager(), "taskDetails");
        }
    }

    @Override public void onDone(Task t) {
        if (isOccurrenceDisplay(t)) {
            RepeatedTaskOccurence oc = mapDisplayTaskIdToOccurrence(t);
            occService.markDone(oc).addOnSuccessListener(x -> loadData());
        } else {
            new TaskService().markDone(t).addOnSuccessListener(x -> loadData());
        }
    }

    @Override public void onCancel(Task t) {
        if (isOccurrenceDisplay(t)) {
            RepeatedTaskOccurence oc = mapDisplayTaskIdToOccurrence(t);
            occService.markCanceled(oc).addOnSuccessListener(x -> loadData());
        } else {
            new TaskService().markCanceled(t).addOnSuccessListener(x -> loadData());
        }
    }

    private boolean isOccurrenceDisplay(@NonNull Task t) {
        String id = t.getId();
        return id != null && id.startsWith("occ:");
    }

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
