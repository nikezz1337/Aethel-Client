package dev.ethereal.api.utils.math;

import lombok.Setter;

public class TimerUtils {
    private static final ThreadLocal<Long> NOW_OVERRIDE = new ThreadLocal<>();
    @Setter private long startTime = now();

    public static long now() {
        Long override = NOW_OVERRIDE.get();
        return override != null ? override : System.currentTimeMillis();
    }

    public static void setNowOverride(Long timeMs) {
        if (timeMs == null) {
            NOW_OVERRIDE.remove();
        } else {
            NOW_OVERRIDE.set(timeMs);
        }
    }

    public void reset() {
        startTime = now();
    }

    public boolean passed(long time) {
        return now() - startTime > time;
    }

    public long getElapsed() {
        return now() - startTime;
    }
    public boolean hasTimeElapsed() {
        return this.startTime < now();
    }
}
