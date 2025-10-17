package com.example.habitgame.utils;

import com.example.habitgame.model.Task;
import com.example.habitgame.model.TaskStatus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Generiše instance pojavljivanja ponavljajućih zadataka.
 * Podržane jedinice: "dan", "nedelja", "mesec" (i eng: "day","week","month").
 */
public final class RecurrenceScheduler {

    private RecurrenceScheduler() {}

    /**
     * Ekspanzija u zadatom opsegu [fromMs, toMs] (oba uključiva).
     * Ako Task nije ponavljajući, vrati najviše jednu instancu (ako je u opsegu).
     */
    public static List<Long> generateOccurrencesInRange(Task task, long fromMs, long toMs) {
        List<Long> out = new ArrayList<>();
        if (task == null) return out;

        fromMs = DateUtils.ensureMillis(fromMs);
        toMs   = DateUtils.ensureMillis(toMs);

        // 1) Non-repeating → jedna eventualna pojava (executionTime ili startDate)
        if (!isRepeatingLike(task)) {
            Long when = task.getExecutionTime() != null ? task.getExecutionTime() : task.getStartDate();
            if (when == null) return out;
            long occ = DateUtils.normalizeToMidnight(when);
            long wFrom = DateUtils.normalizeToMidnight(fromMs);
            long wTo   = DateUtils.normalizeToMidnight(toMs);
            if (occ >= wFrom && occ <= wTo) {
                out.add(occ);
            }
            return out;
        }

        // 2) Repeating
        if (task.getStartDate() == null) return out;

        long start = DateUtils.normalizeToMidnight(task.getStartDate());
        Long rawEnd = task.getEndDate();
        long end = (rawEnd != null) ? DateUtils.normalizeToMidnight(rawEnd) : toMs;

        // Ograniči seriju na traženi prozor
        long windowStart = DateUtils.normalizeToMidnight(fromMs);
        long windowEnd   = DateUtils.normalizeToMidnight(toMs);
        long seriesEnd   = Math.min(end, windowEnd);

        if (start > seriesEnd) return out; // sve posle opsega

        int interval = Math.max(1, safeInt(task.getRepeatInterval(), 1));
        Unit unit = Unit.from(task.getRepeatUnit());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(start);
        normalizeMidnight(cal);

        // Ako start < windowStart, fast-forward do prve pojave >= windowStart
        fastForward(cal, windowStart, interval, unit);

        while (cal.getTimeInMillis() <= seriesEnd) {
            out.add(cal.getTimeInMillis());
            addInterval(cal, interval, unit);
        }
        return out;
    }

    /**
     * Jednostavna varijanta: generiše od danas do task.endDate (ili +10y ako je null).
     */
    public static List<Long> generateUpcoming(Task task) {
        long from = DateUtils.startOfToday();
        long to;
        if (task != null && task.getEndDate() != null) {
            to = DateUtils.normalizeToMidnight(task.getEndDate());
        } else {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(from);
            c.add(Calendar.YEAR, 10);
            to = c.getTimeInMillis();
        }
        return generateOccurrencesInRange(task, from, to);
    }

    /**
     * Pravi "vizuelne" instance Task-a (kopije sa postavljenim executionTime).
     * **VAŽNO**: Instanci eksplicitno postavljamo isRepeating=true i kopiramo repeatInterval/Unit,
     * kako bi UI tretirao pojavu kao ponavljajući zadatak (Pauziraj/Aktiviraj dugmad).
     */
    public static List<Task> expandTaskAsInstances(Task source, long fromMs, long toMs) {
        List<Task> list = new ArrayList<>();
        for (Long when : generateOccurrencesInRange(source, fromMs, toMs)) {
            Task copy = new Task();
            copy.setId(source.getId());
            copy.setUserId(source.getUserId());
            copy.setName(source.getName());
            copy.setDescription(source.getDescription());
            copy.setCategoryId(source.getCategoryId());
            copy.setWeight(source.getWeight());
            copy.setImportance(source.getImportance());
            copy.setXpValue(source.getXpValue());

            copy.setExecutionTime(when);

            // <<< KLJUČNO: tretiramo instancu kao ponavljajuću
            copy.setIsRepeating(true);
            copy.setStartDate(source.getStartDate());
            copy.setEndDate(source.getEndDate());
            copy.setRepeatInterval(source.getRepeatInterval());
            copy.setRepeatUnit(source.getRepeatUnit());

            copy.setIsCompleted(false);
            copy.setCreationTimestamp(source.getCreationTimestamp());
            copy.setLastCompletionTimestamp(source.getLastCompletionTimestamp());
            copy.setCompletionsTodayCount(source.getCompletionsTodayCount());

            copy.setStatus(source.getStatus() == null ? TaskStatus.AKTIVAN : source.getStatus());
            list.add(copy);
        }
        return list;
    }

    // ---------------- internals ----------------

    private static boolean isRepeatingLike(Task t) {
        if (Boolean.TRUE.equals(t.getIsRepeating())) return true;
        Integer ri = t.getRepeatInterval();
        String ru = t.getRepeatUnit();
        return ri != null && ri > 0 && ru != null && ru.trim().length() > 0;
    }

    private enum Unit { DAY, WEEK, MONTH;

        static Unit from(String raw) {
            if (raw == null) return DAY;
            String u = raw.toLowerCase(Locale.ROOT).trim();
            switch (u) {
                case "nedelja":
                case "week":   return WEEK;
                case "mesec":
                case "month":  return MONTH;
                case "dan":
                case "day":
                default:       return DAY;
            }
        }
    }

    private static void addInterval(Calendar cal, int interval, Unit unit) {
        switch (unit) {
            case WEEK:
                cal.add(Calendar.WEEK_OF_YEAR, interval);
                break;
            case MONTH:
                cal.add(Calendar.MONTH, interval);
                break;
            case DAY:
            default:
                cal.add(Calendar.DAY_OF_YEAR,  interval);
                break;
        }
        normalizeMidnight(cal);
    }

    /** Pomeranje do prve pojave >= windowStart da izbegnemo veliki broj koraka. */
    private static void fastForward(Calendar cal, long windowStart, int interval, Unit unit) {
        if (cal.getTimeInMillis() >= windowStart) return;

        switch (unit) {
            case DAY: {
                long diffDays = (windowStart - cal.getTimeInMillis()) / (24L * 60 * 60 * 1000);
                long steps = diffDays / interval;
                if (steps > 0) cal.add(Calendar.DAY_OF_YEAR, (int) (steps * interval));
                while (cal.getTimeInMillis() < windowStart) addInterval(cal, interval, unit);
                break;
            }
            case WEEK: {
                long diffDays = (windowStart - cal.getTimeInMillis()) / (24L * 60 * 60 * 1000);
                long diffWeeks = diffDays / 7L;
                long steps = diffWeeks / interval;
                if (steps > 0) cal.add(Calendar.WEEK_OF_YEAR, (int) (steps * interval));
                while (cal.getTimeInMillis() < windowStart) addInterval(cal, interval, unit);
                break;
            }
            case MONTH: {
                Calendar ws = Calendar.getInstance();
                ws.setTimeInMillis(windowStart);
                int ymStart = cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH);
                int ymWindow= ws.get(Calendar.YEAR)  * 12 + ws.get(Calendar.MONTH);
                int diffMonths = ymWindow - ymStart;
                if (diffMonths > 0) {
                    int steps = diffMonths / interval;
                    if (steps > 0) cal.add(Calendar.MONTH, steps * interval);
                }
                while (cal.getTimeInMillis() < windowStart) addInterval(cal, interval, unit);
                break;
            }
        }
    }

    private static void normalizeMidnight(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private static int safeInt(Integer v, int def) {
        return (v == null ? def : v);
    }
}
