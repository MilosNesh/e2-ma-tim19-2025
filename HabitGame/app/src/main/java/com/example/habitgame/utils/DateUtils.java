package com.example.habitgame.utils;

import java.util.Calendar;
import java.util.TimeZone;

public final class DateUtils {

    private DateUtils() {}

    public static long ensureMillis(long ts) {
        return ts < 1_000_000_000_000L ? ts * 1000L : ts;
    }

    public static long normalizeToMidnight(long ts) {
        ts = ensureMillis(ts);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
    public static boolean isTodayOrFuture(long ts) {
        ts = ensureMillis(ts);
        return ts >= startOfToday();
    }

}
