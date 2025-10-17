package com.example.habitgame.utils;

import java.util.Calendar;
import java.util.TimeZone;

public final class DateUtils {

    private DateUtils() {}

    /** Ako je timestamp verovatno u sekundama, pretvara ga u milisekunde. */
    public static long ensureMillis(long ts) {
        return ts < 1_000_000_000_000L ? ts * 1000L : ts;
    }

    /** Ponoć (00:00:00.000) za dati timestamp, u sistemskoj zoni. */
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

    /** Ponoć današnjeg dana u milisekundama. */
    public static long startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Ponoć današnjeg dana u zadatoj zoni. */
    public static long startOfToday(TimeZone tz) {
        Calendar c = Calendar.getInstance(tz);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** true ako je ts danas ili u budućnosti (od ponoći naviše). */
    public static boolean isTodayOrFuture(long ts) {
        ts = ensureMillis(ts);
        return ts >= startOfToday();
    }

    /** Clampa vrednost u [min, max]. */
    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
