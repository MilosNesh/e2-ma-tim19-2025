package com.example.habitgame.utils;

import com.example.habitgame.model.Task;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RecurrenceScheduler {

    public static List<Long> generateRecurrenceTimestamps(Task task) {
        List<Long> out = new ArrayList<>();

        if (!task.getIsRepeating() || task.getRepeatInterval() == null || task.getRepeatUnit() == null) {
            if (task.getExecutionTime() != null) out.add(task.getExecutionTime());
            return out;
        }

        if (task.getStartDate() == null) return out;

        long start = normalizeToMidnight(task.getStartDate());
        Long end = task.getEndDate() != null ? normalizeToMidnight(task.getEndDate()) : null;

        int interval = Math.max(1, task.getRepeatInterval());
        String unit = task.getRepeatUnit().toLowerCase().trim();

        int calendarField;
        if ("dan".equals(unit)) {
            calendarField = Calendar.DAY_OF_YEAR;
        } else if ("nedelja".equals(unit)) {
            calendarField = Calendar.WEEK_OF_YEAR;
        } else {
            return out; // nepoznata jedinica
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(start);

        // Ako nema end, veštački ga postavi 10 godina kasnije (ili ograničiš na broj instanci)
        if (end == null) {
            Calendar tmp = (Calendar) cal.clone();
            tmp.add(Calendar.YEAR, 10);
            end = tmp.getTimeInMillis();
        }

        // Uključi start, stani kada pređe end
        while (cal.getTimeInMillis() <= end) {
            out.add(cal.getTimeInMillis());
            cal.add(calendarField, interval);
        }

        return out;
    }

    private static long normalizeToMidnight(Long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}